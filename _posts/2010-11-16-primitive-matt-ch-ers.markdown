---
layout: post
title: "Primitive Matt(ch)ers?"
date: 2010-11-16 20:34
comments: true
categories: [JUnit, Hamcrest]
---

The [Hamcrest project](http://code.google.com/p/hamcrest/) provides a large number of matchers, i.e. declaratively defined predicates. Prominent uses of these matchers include testing and mocking libraries like JUnit and jMock, respectively.

One of the benefits of using `assertThat()` and Hamcrest matchers is that assertions become very readable. Unfortunately, you often have to rely on a questionable Java mechanism: auto boxing/unboxing.

<!--more-->

### How to use matchers?

So, here is a very simple example of how to use a matcher:

```java
@Test
public void onePlusOneIsTwo() {
    assertThat(1 + 1, is(2));
}
```

Auto boxing and unboxing have been introduced in Java 5 to ease the use of primitive types and their counterparts: *real* objects (a.k.a. reference types). However, especially unboxing can lead to hidden NullPointerExceptions and thus is discouraged by many developers. For details see [Autoboxing is Evil](http://pboop.wordpress.com/2010/09/22/autoboxing-is-evil/) by Nicole Rauch and Andreas Leidig.

For this reason, the Eclipse Java compiler optionally shows warnings whenever boxing or unboxing occurs. While it is certainly a good idea to enable this warning, it also puts markers on code that is perfectly sane, like the test case above. To prevent un-/boxing and use matchers at the same time, one can go back to pre-Java 5 times and convert the
primitive literals explicitly:

```java
@Test
public void onePlusOneIsTwo() {
    assertThat(Integer.valueOf(1 + 1), is(Integer.valueOf(2)));
}
```

However, this is not readable anymore!

### Why do we need boxing in the first place?

When using `assertThat()` we need boxing for two reasons. First, there is no definition of `assertThat()` for primitive types, only for reference types:

```java
<T> void assertThat(T actual, Matcher<T> matcher)
```

Well, why don't we overload `assertThat()` with separate method definitions for each primitive type, you might say.

```java
void assertThat(int actual, Matcher<int> matcher)
```

Java's generics do not allow primitive types like `int` as type arguments, only reference types are allowed.

### Hmm, anything we *can* do?

Yes! So how about

```java
void assertThat(int actual, Matcher<Integer> matcher) {
    assertThat(Integer.valueOf(actual), matcher);
}
```

Without using auto boxing, we can now write:

```java
@Test
public void onePlusOneIsTwo() {
    assertThat(1 + 1, is(Integer.valueOf(2)));
}
```

That solves half of our problem: We got rid of the first boxing by overloading `assertEquals()`. However, we still need to explicity convert our `int` to `Integer` when calling the matcher factory method. Thus, we also need to overload the `is()` method:

```java
Matcher<Integer> is(int value) {
    return is(Integer.valueOf(value));
}
```

Problem solved! Really? Obviously, it requires a lot of work do define extra matcher factory methods for every combination of primitive type and matcher.

### Solution: generate it!

Hamcrest already allows to [generate matcher libraries](http://code.google.com/p/hamcrest/wiki/Tutorial#Sugar_generation), i.e. classes that collect all static factory methods for easy access at a single entry point. So, we could simply generate extra methods for every matcher. To stay with the example from above, if we have an
unrestricted `Matcher<T>`, we would generate the following eight overloading methods:

```java
Matcher<Byte> is(byte value) {
    return is(Byte.valueOf(value));
}
Matcher<Short> is(short value) {
    return is(Short.valueOf(value));
}
Matcher<Integer> is(int value) {
    return is(Integer.valueOf(value));
}
Matcher<Long> is(long value) {
    return is(Long.valueOf(value));
}
Matcher<Float> is(float value) {
    return is(Float.valueOf(value));
}
Matcher<Double> is(double value) {
    return is(Double.valueOf(value));
}
Matcher<Boolean> is(boolean value) {
    return is(Boolean.valueOf(value));
}
Matcher<Character> is(char value) {
    return is(Character.valueOf(value));
}
```

A matcher declaration which uses the object representation of a primitive type, e.g. `Matcher<Integer>`, is a special case that is even more simple. We would only need to generate a single extra method, such as

```java
Matcher<Integer> zero(int value) {
    return zero(Integer.valueOf(value));
}
```

Summing up, it would be great if Hamcrest provided built-in support for primitive types.

### Update

My proposal was [rejected](http://code.google.com/p/hamcrest/issues/detail?id=130) by the Hamcrest maintainers.
