---
title: "STF Milestone 6: Improved Kotlin support"
date: 2025-08-21 00:01
categories: [Sovereign Tech Fund, JUnit]
lang: en
ref: 2025-08-21-stf-milestone-6-improved-kotlin-support
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

Kotlin is a highly popular language for JVM applications (and beyond).
Not only is it the default language for Android apps, it has also become increasingly popular for backend applications.
JUnit Jupiter has always supported writing tests in Kotlin and provides Kotlin-specific assertions.
However, there were some long-standing and highly-voted issues for improving the usability when writing tests and extensions in Kotlin which have now been addressed in this STF milestone.<!--more-->
{:.lead}

The improvements made in this milestone fall into the following categories:

* [Kotlin contracts](#kotlin-contracts)
* [JSpecify nullness annotations](#jspecify-nullness-annotations)
* [Kotlin suspending functions](#kotlin-suspending-functions)
* [Kotlin `Sequence` support](#kotlin-sequence-support)

## Kotlin contracts

Kotlin is a statically typed language and the Kotlin compiler has powerful static analysis capabilities.
One of them and an often cited difference between Java and Kotlin is its null safety.

Given a `Person` class, a Kotlin variable can be of type `Person` or `Person?` where the former does not allow assigning `null` while the latter does.
When using a variable of a nullable type such as `Person?`, the compiler requires code to deal with the possibility of the value being `null`.
For example, to access the `firstName` property of `Person` class on `person: Person?`, one can't just use `person.firstName`.
Instead, one usually either writes `person!!.firstName` (which will throw a `NullPointerException` in case `person` is `null`) or `person?.firstName` (which will evaluate to `null` if `person` is `null` and `person.firstName` otherwise).

To make dealing with nullable types easier, Kotlin has a feature called "smart casts".
If the Kotlin compiler can deduce that a value can no longer be `null` because it has already been checked for nullness, it allows using it directly without the `!!.` or `?.` operators.
To support the compiler, Kotlin functions can define [contracts](https://kotlinlang.org/docs/whatsnew13.html#contracts) that describe their behavior.

As part of this milestone, JUnit's Kotlin-specific assertion functions have been enhanced to define such contracts.
For example, the `assertNotNull` function now defines the following contract:

```kotlin
fun assertNotNull(actual: Any?) {
    contract {
        returns() implies (actual != null)
    }
    // ...
}
```

The contract states that if the function returns normally, meaning without throwing an assertion error, that its `actual` argument has been checked for nullness and that subsequent code can assume it's not `null`.
The following example demonstrates why this can be useful:

```kotlin
val person: Person? = findPerson("John", "Doe")
assertNotNull(person)
assertEquals("John", person.firstName)
assertEquals("Doe", person.lastName)
```

In both calls to `assertEquals`, `person` is dereferenced without using `!!.` or `?.` since the compiler knows it can no longer be `null` due to the prior call of `assertNotNull`.

## JSpecify nullness annotations

In addition to contracts for Kotlin code, [JUnit 6.0.0-RC1](https://docs.junit.org/6.0.0-RC1/release-notes/) started using [JSpecify](https://jspecify.dev/) to indicate nullness throughout its Java APIs.
The Kotlin compiler supports JSpecify's annotations which makes writing Kotlin code using JUnit's APIs more safe and idiomatic.

Of course, there are also benefits when writing code in Java.
JSpecify is supported in IDEs such as IntelliJ IDEA so you may notice new warnings when programming against JUnit's APIs.
To check nullability during your build, please check out [Error Prone](https://errorprone.info/) and [NullAway](https://github.com/uber/NullAway).

## Kotlin suspending functions

Kotlin has first-class language support for concurrency and asynchronous programming via [coroutines](https://kotlinlang.org/docs/coroutines-overview.html).
One such language feature is the `suspend` keyword that can be applied to function definitions.
Such suspending functions allow writing asynchronous code in regular sequential style but can pause and resume without blocking a thread.

Prior to [JUnit 6.0.0-RC1](https://docs.junit.org/6.0.0-RC1/release-notes/), writing tests for suspending functions with JUnit Jupiter was not straightforward.
Since suspending functions can only be called from other suspending functions, it felt natural to simply add the `suspend` keyword to the `@Test` method as well.
However, that didn't work because `@Test` methods with the `suspend` modifier were silently ignored by JUnit due to the way the Kotlin compiler generates JVM bytecode for them.
For example, the `@Test suspend fun test() { /*...*/ }` function will be compiled to the equivalent of the following Java method:
```java
@Test
public final java.lang.Object test(kotlin.coroutines.Continuation<? super kotlin.Unit> c) {
	/*...*/
}
```
As discussed in the [previous post](/blog/2025/08/16/stf-milestone-5-discovery-issues/), `@Test` methods must have a `void` return type.
Consequently, [JUnit 5.13.0](https://docs.junit.org/5.13.0/release-notes/) started reporting discovery issues of severity "warning" in such cases.
However, wouldn't it be better if it "just worked"?

Kotlin core libraries provide different ways of executing coroutines, meaning code that calls suspending functions.
One of the simplest is `runBlocking` ([docs](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/run-blocking.html)) from `kotlinx.coroutines`.
It runs the supplied code and blocks the current thread until its completion.
Starting with [JUnit 6.0.0-RC1](https://docs.junit.org/6.0.0-RC1/release-notes/), `@Test` functions written in Kotlin may now carry the `suspend` modifier.
Behind the scenes, JUnit Jupiter will wrap the function body in a call to `runBlocking`.
For example, the following two `@Test` methods are equivalent.

```kotlin
class CoroutineTests {
    
    @Test
    suspend fun test1() {
        someSuspendingFunction()
    }
    
    @Test
    fun test2(): Unit = runBlocking {
        someSuspendingFunction()
    }
    
    suspend fun someSuspendingFunction(): String {
        // ...
    }
}
```

If you'd rather use one of the alternatives to `runBlocking`, such as `runTest` ([docs](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/kotlinx.coroutines.test/run-test.html)), you can do so explicitly as follows.

```kotlin
class CustomCoroutineTests {
    @Test
    fun test(): Unit = runTest {
        someSuspendingFunction()
    }
}
```

## Kotlin `Sequence` support

Kotlin's `Sequence` ([docs](https://kotlinlang.org/docs/sequences.html)) abstraction is similar to Java's `Iterable`.
The main difference lies in how it's used in Kotlin's standard library.
Methods operating on `Iterable` are typically eager whereas those using `Sequence` are lazy.
For example, the `Iterable<T>.map { ... }` extension function applies the supplied function to all elements of the `Iterable` and returns a `List<R>`.
The corresponding `Sequence<T>.map { ... }` extension function returns another `Sequence<R>` and does not apply the supplied function until a terminal operation like `toList()` is called.
So, from a Java perspective, it's more similar to `Stream`.

JUnit supports conversion of a variety of types to `Stream` for use with [dynamic](https://docs.junit.org/current/user-guide/#writing-tests-dynamic-tests) or [parameterized](https://docs.junit.org/current/user-guide/#writing-tests-parameterized-tests) tests.
Prior to [JUnit 5.13.0](https://docs.junit.org/5.13.0/release-notes/), this included different types of `Stream`, `Collection`, `Iterable`, `Iterator`, and all array types.
This list has been extended to support all types defining an `Iterator iterator()` method, including Kotlin's `Sequence`.
For example, Kotlin sequences can now be used with `@MethodSource` or `@FieldSource`.

```kotlin
object SequenceTests {

    @JvmStatic
    val data = sequenceOf(
        arguments(1, Month.JANUARY),
        arguments(3, Month.MARCH),
        arguments(8, Month.AUGUST),
        arguments(5, Month.MAY),
        arguments(12, Month.DECEMBER)
    )

    @ParameterizedTest
    @FieldSource("data")
    fun test(value: Int, month: Month) {
        assertEquals(value, month.value)
    }
}
```

## Wrapping up

Thanks to these improvements writing JUnit Jupiter tests in Kotlin has just gotten more convenient and idiomatic.
Please give [JUnit 6.0.0-RC1](https://docs.junit.org/6.0.0-RC1/release-notes/) a try and let us know what you think and provide feedback on [GitHub](https://github.com/junit-team/junit-framework/).
