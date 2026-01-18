---
title: "STF Milestone 8: Improved parallel test execution"
date: 2026-01-18 00:01
categories: [Sovereign Tech Fund, JUnit]
lang: en
ref: 2026-01-18-stf-milestone-8-improved-parallel-test-execution
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

Since its introduction in JUnit 5.3, parallel test execution has become a popular feature of the JUnit Platform.
When enabled, it can significantly speed up test execution for many projects.
However, there were still some rough edges and long-standing issues that needed to be addressed.
Therefore, [this milestone](https://github.com/junit-team/junit-framework/issues/5115) focused on improving the parallel test execution capabilities of JUnit.
<!--more-->
{:.lead}

### Vintage engine

JUnit Platform's parallel execution support works for all test engines extending `HierarchicalTestEngine`, such as JUnit Jupiter, Spock, and Cucumber.
Notably missing from that list is the JUnit Vintage engine, which runs JUnit 3 and JUnit 4 tests.
While the Vintage engine is only intended as a temporary migration aid, there are projects that will probably never migrate all their tests to JUnit Jupiter.
To help projects that use both the Vintage engine and Jupiter (or another testing framework based on the JUnit Platform), the Vintage engine has been enhanced to support parallel test execution as well, starting with version 5.12.0.
As documented in the [User Guide](https://docs.junit.org/6.0.2/migrating-from-junit4.html#parallel-execution), the behavior can be enabled and configured via the following configuration parameters:

```properties
junit.vintage.execution.parallel.enabled=true
junit.vintage.execution.parallel.classes=true
junit.vintage.execution.parallel.methods=true
junit.vintage.execution.parallel.pool-size=4
```

Thanks to contributor [Yongjun Hong](https://github.com/YongGoose) for implementing this feature!

### Resource locks

[Resource locks](https://docs.junit.org/6.0.2/writing-tests/parallel-execution.html#synchronization) are a declarative mechanism to control which tests may run in parallel.
For example, the test methods in the following example class use resource locks to prevent conflicting concurrent access to system properties:

```java
@Execution(CONCURRENT)
class StaticSharedResourcesDemo {

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ)
    void customPropertyIsNotSetByDefault() {
        assertNull(System.getProperty("my.prop"));
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ)
    void anotherCustomPropertyIsNotSetByDefault() {
        assertNull(System.getProperty("my.other.prop"));
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    void canSetCustomPropertyToApple() {
        System.setProperty("my.prop", "apple");
        assertEquals("apple", System.getProperty("my.prop"));
    }
}
```

While `customPropertyIsNotSetByDefault()` cannot run concurrently with `canSetCustomPropertyToApple()`, it may run in parallel with `anotherCustomPropertyIsNotSetByDefault()`.
In order to express that, prior to JUnit 5.12, every test method in this test class needs to be annotated with a `@ResourceLock` annotation.
To reduce this boilerplate, a new `target` attribute has been added to the `@ResourceLock` annotation.
It allows specifying that the lock applies to all test methods in the annotated class.
A test method such as `canSetCustomPropertyToApple()` may define a lock with the same `value` and a stricter `mode`, though.

```java
@Execution(CONCURRENT)
@ResourceLock(value = SYSTEM_PROPERTIES, mode = READ, target = CHILDREN)
class StaticSharedResourcesDemo {

    @Test
    void customPropertyIsNotSetByDefault() {
        assertNull(System.getProperty("my.prop"));
    }

    @Test
    void anotherCustomPropertyIsNotSetByDefault() {
        assertNull(System.getProperty("my.other.prop"));
    }

    @Test
    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    void canSetCustomPropertyToApple() {
        System.setProperty("my.prop", "apple");
        assertEquals("apple", System.getProperty("my.prop"));
    }
}
```

Another improvement was made for test engines such as Cucumber where parent containers have no execution behavior of their own but solely exist to group child tests.
Similar to Jupiter's `@Isolated` annotation, Cucumber scenarios can be annotated with `@isolated` to indicate that they should not run in parallel with other tests/scenarios.
However, the JUnit Platform always pulled up that lock to the test class/feature level since a test class in Jupiter or Spock can have its own execution behavior (for example, a Jupiter `@AfterAll` method).
This made it impossible to run Cucumber features in parallel if one of them contained an isolated scenario.
The Cucumber engine will be able to configure this behavior once the new API is released in JUnit 6.1.

### <s>Open heart surgery</s> Reimplementing parallel test execution

The initial implementation of parallel test execution in the JUnit Platform is based on Java's `ForkJoinPool`.
Given the hierarchical nature of the test tree, this seemed like a natural fit.
For example, a test class would perform its setup, then submit its test methods to the pool for execution (_fork_), wait for them to finish (_join_), and finally perform its teardown.

However, over time, it became clear that this implementation decision is causing issues in certain scenarios.
For example, when the code under test also uses `ForkJoinPool` for its own parallelism, the pool used to run tests in parallel would suddenly spawn additional threads.
This could lead to more tests running in parallel than configured, causing resource exhaustion and out-of-memory errors.
Another problem was unwanted work-stealing in the presence of resource locks.
We had to implement special handling to prevent tests holding a set of resource locks from stealing work from other tests with conflicting locks.

There have been several suggestions over the years on how to deal with these issues.
Most recently, a contributor proposed a solution that used a regular thread pool _in addition_ to the existing `ForkJoinPool`.
This milestone finally gave me a chance to explore this idea.
However, I soon realized that using two thread pools would not be a good solution.
Not only would it double the number of threads and require context switching between the two pools, it would also add significant complexity to the implementation.
Therefore, I set out to reimplement parallel test execution using a regular thread pool with "simple" work-stealing support.
As you can imagine, that was quite an undertaking, and difficult to test properly.
Luckily, I had help!
Both, [Leonard Brünings](https://github.com/leonard84) from Spock and [Rien Korstanje](https://github.com/mpkorstanje) from Cucumber helped challenge, test, and improve the new implementation.
A huge thanks to both of them!

The new implementation will be an opt-in feature in JUnit 6.1.
You can already give it a try today by using version [6.1.0-M1](https://docs.junit.org/6.1.0-M1/release-notes.html#v6.1.0-M1) (see [User Guide](https://docs.junit.org/6.1.0-M1/writing-tests/parallel-execution.html#config-executor-service) for details).
If we receive promising feedback in 6.1, we will switch the default to the new implementation in 6.2.
Looking forward to your feedback!
