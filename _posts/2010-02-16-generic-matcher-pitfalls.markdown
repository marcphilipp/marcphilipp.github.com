---
title: "Generic Matcher Pitfalls"
date: 2010-02-16 22:45
comments: true
categories: [JUnit, Hamcrest]
lang: en
ref: post-generic-matcher-pitfalls
---

Using [Hamcrest](http://code.google.com/p/hamcrest/) matchers in combination with `assertThat` allows for more fluid specification of JUnit assertions.

Recently, while working on the backend of [Project Usus](http://projectusus.org/), we needed a simple matcher, that would test whether a given set is empty. At the time, we reused a set matcher we had already written a few minutes earlier.

<!--more-->

Today, I had another look at the pre-defined matchers that come with Hamcrest and found the `empty()` matcher in `org.hamcrest.Matchers`. Since I'm not concerned with the actual implementation (at least for now), I'll just give you the factory method:

```java
@Factory
public static <E> Matcher<Collection<E>> empty() {
    return new IsEmptyCollection<E>();
}
```

Great, I thought. So I readily changed our tests to use the pre-defined matcher…

```java
assertThat(new TreeSet<String>(), empty());
```

However, this yielded a compile error because the compiler could not infer the type parameter of the method. It *did* work when stating the type parameter of the static method explicitly:

```java
assertThat(new TreeSet<String>(), Matchers.<String>empty());
```

But that looked horrible. My first shot was to define an own factory method…

```java
@Factory
public static <E> Matcher<Collection<E>> emptyOf(Class<E> clazz) {
    return new IsEmptyCollection<E>();
}
```

…that can be used like this:

```java
assertThat(new TreeSet<String>(), emptyOf(String.class));
```

I was still not very pleased with the solution. Even more since it does not matter at all what kind of objects are inside the collection to determine whether it is empty. After playing around for a little while I came up with this solution:

```java
public class IsEmptyCollection extends TypeSafeMatcher<Collection<?>> {

    @Override
    protected boolean matchesSafely(Collection<?> collection) {
        return collection.isEmpty();
    }

    public void describeTo(Description description) {
        description.appendText("empty");
    }

    @Factory
    public static Matcher<Collection<?>> empty() {
        return new IsEmptyCollection();
    }
}
```

In conclusion, I think it is not trivial to write usable generic matchers. Therefore, avoid generics when you don't need them!
