---
title: "Applying DRY to JUnit Categories"
date: 2010-03-13 22:08
comments: true
categories: [JUnit]
lang: en
ref: post-applying-dry-to-junit-categories
---

Long awaited, [JUnit 4.8](http://junit.org/junit4/doc/ReleaseNotes4.8.html) introduced support for categorizing test cases.

A category marker is simply a class or interface, e.g.

```java
public interface SlowTests {}
```

<!--more-->

Tests can be marked using the `@Category` annotation:

```java
public class A {
    @Test
    public void a() {}

    @Category(SlowTests.class)
    @Test
    public void b() {}
}
```

The annotation works both on methods and classes:

```java
@Category(SlowTests.class)
public class B {
    @Test
    public void c() {}
}
```

Test suites that include or exclude the `SlowTests` category are defined by specifying the `Categories` runner and using the `@ExcludeCategory` or `@IncludeCategory` annotation, respectively:

```java
@RunWith(Categories.class)
@SuiteClasses( { A.class, B.class })
@ExcludeCategory(SlowTests.class)
public class AllFastTests extends AllTests {}

@RunWith(Categories.class)
@SuiteClasses( { A.class, B.class })
@IncludeCategory(SlowTests.class)
public class AllSlowTests extends AllTests {}
```

In this example, `AllFastTests` would execute only `A.a` while `AllSlowTests` would ignore `A.a` but run `A.b` and `B.c`.

However, there is a major issue in the above suite declarations: they violate the [DRY](http://c2.com/cgi/wiki?DontRepeatYourself "Don't Repeat Yourself") principle. Both test suites list all test classes in the `@SuiteClasses` annotation. While it seems feasible to maintain the list of test classes at two locations for a small number of classes, it certainly is not a viable option in a real-world setting, especially when there are multiple categories.

Fortunately, there is a simple solution: use inheritance. You can define the list of test classes once in a normal test suite …

```java
@RunWith(Suite.class)
@SuiteClasses( { A.class, B.class })
public class AllTests {}
```

… and declare subclasses that filter the list of classes by category:

```java
@RunWith(Categories.class)
@ExcludeCategory(SlowTests.class)
public class AllFastTests extends AllTests {}

@RunWith(Categories.class)
@IncludeCategory(SlowTests.class)
public class AllSlowTests extends AllTests {}
```
