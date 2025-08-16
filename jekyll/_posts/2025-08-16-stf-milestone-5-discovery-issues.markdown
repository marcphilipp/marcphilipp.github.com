---
title: "STF Milestone 5: Discovery issues"
date: 2025-08-16 00:01
categories: [Sovereign Tech Fund, JUnit 5]
lang: en
ref: 2025-08-16-stf-milestone-5-discovery-issues
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

This milestone was all about finally resolving issue [#242](https://github.com/junit-team/junit-framework/issues/242) by introducing an error handling mechanism for test discovery.
As you can tell by the low issue number (we're currently at #4840) it has been a long time coming.
Since introducing the discovery phase in an early version of JUnit 5, we had seen the need for being able to report "discovery issues" such as errors and warnings but also info-level messages to users.<!--more-->
{:.lead}

Have you ever written a test and noticed at some later point that it wasn't actually being executed?
I know, it sounds like a real nightmare.
But, can you spot what's wrong with these test methods?

```java
@Test // Java
int test() {
    return 42;
}
```
```kotlin
@Test // Kotlin
fun test(): Nothing = fail()
```

They look like regular tests, don't they?
Well, actually, `@Test` methods must be declared having a `void` return type (`Unit` in Kotlin)!
During the test discovery phase, the JUnit Jupiter test engine looks for methods that are annotated with `@Test` and have a `void` return type.
Before version 5.13, if it found a malformed declaration, such as the ones above, it silently discarded them -- even if they were clearly intended to be test methods.

To be fair, IDEs such as IntelliJ often provide inspections to help avoid running into such issues.
However, such IDE-only warnings are easy to overlook, especially in a large codebase.

Therefore, rather than keeping you up at night, JUnit 5.13 introduced the concept of ["discovery issues"](https://docs.junit.org/current/user-guide/#running-tests-discovery-issues) and a two-part mechanism for reporting them.
First, test engines can [report issues](https://docs.junit.org/current/user-guide/#test-engines-discovery-issues) such as malformed declarations during test discovery.
Second, the JUnit Platform takes care of reporting them to users.
It logs all non-critical issues and, if there are any critical issues, test execution will fail for that test engine.
The severity that's considered "critical" is configurable via the `junit.platform.discovery.issue.severity.critical` [configuration parameter](https://docs.junit.org/current/user-guide/#running-tests-config-params).
Currently, the default value is `ERROR`, but that may be changed in a future release.

All test engines provided by JUnit (Jupiter, Vintage, and Suite) already make use of this feature to report a variety of issues.
For example, the Jupiter engine will now report a warning for malformed test and lifecycle method declarations such as the ones shown above.
For the full list, please consult issue [#242](https://github.com/junit-team/junit-framework/issues/242) on GitHub.

We've introduced this mechanism at a stage when the JUnit Platform and its test engines were already widely used.
Therefore, it was probably inevitable that the initial 5.13.0 release reported a few false-positives.
For example, in 5.13.0 abstract test methods were overeagerly reported as being problematic, but we've since learned about use cases for them in the wild.
Those and other checks have since been refined and improved in 5.13.x patch releases.

On the other hand, we've already seen a bunch of projects that had actual problems that needed addressing reported to them after they updated.
For example, one project had lots of inner classes that contained test methods but those inner classes were not annotated with `@Nested` so those tests were never executed.
That's exactly the kind of problem we wanted to address by introducing this mechanism!

If you haven't yet updated to JUnit 5.13 or later, please do and look out for any reported discovery issues.
To surface all discovery issues in your project, we recommend setting the `junit.platform.discovery.issue.severity.critical` configuration parameter to `INFO` and then running all your tests.
