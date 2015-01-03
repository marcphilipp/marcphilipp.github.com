---
layout: post
title: "Combining SuiteBuilder and ClasspathSuite"
date: 2010-05-13 13:05
comments: true
categories: [JUnit]
---

In a recent [commit](http://github.com/KentBeck/junit/commit/f09cff79b941a525271f3f2838a9742b4c5c8d36) to JUnit Kent Beck and David Saff have added an "alpha-ready implementation of `SuiteBuilder`". As Kent Beck previously described in a [blog post](http://www.threeriversinstitute.org/blog/?p=456), the idea behind the `SuiteBuilder` runner is to use annotations on fields instead of annotations on classes.

<!--more-->

### Limitations of regular test suites

While an annotation can take parameters the arguments must be literals, e.g. constant String values or class literals. For example, the classic `Suite` runner is configured using the `@SuiteClasses` annotation that takes an array of class literals, i.e. the test classes to be run:

{% highlight java %}
@RunWith(Suite.class)
@SuiteClasses({
    SomeTest.class,
    YetAnotherTest.class
})
public class AllTests {}
{% endhighlight %}

Literals have a severe limitation: they must be know at compile-time! Thus, when using the `Suite` runner, there was no way of determining the classes to run by any other means such as scanning the current classpath.

### ClasspathSuite to the rescue

For this original purpose, Johannes Link created the [`ClasspathSuite`](http://johanneslink.net/projects/cpsuite.jsp) runner. Its basic usage is very simple: just specify it using the `@RunWith` annotation. In addition, you can also include test classes in JAR files, filter by class names or types, and so on:

{% highlight java %}
@RunWith(ClasspathSuite.class)
@IncludeJars(true)
@ClassnameFilters({".*Test", "!.*AllTests"})
@BaseTypeFilter(MyBaseTest.class)
public class AllTests {}
{% endhighlight %}

However, the `ClasspathSuite` does not support JUnit's categories as mentioned in an [earlier blog post]({{ root_url }}/blog/2010/03/13/applying-dry-to-junit-categories/). While it could certainly be extended to support the Category-related annotations `@IncludeCategory` and `@ExcludeCategory`, the `SuiteBuilder` offers a more flexible alternative.

### Introducing SuiteBuilder

The `SuiteBuilder` runner is similar to the `Suite` runner, but reads the test classes it is supposed to run from a field of the suite class annotated with `@Classes`. The field can be freely initialized to hold an implementation of the `SuiteBuilder.Classes.Value` interface which simply wraps a collection of classes. E.g., the first example can be rewritten using the `SuiteBuilder`:

{% highlight java %}
@RunWith(SuiteBuilder.class)
public class AllTests {
    @Classes
    public Listed classes =
        new Listed(SomeTest.class, YetAnotherTest.class);
}
{% endhighlight %}

In addition, you can filter the resulting test runners by annotating a field of type `SuiteBuilder.RunnerFilter.Value` with `@RunnerFilter`. For example, the latest commit included a `CategoryFilter` that filters tests by category:

{% highlight java %}
@RunWith(SuiteBuilder.class)
public class OnlyYes {
    @Classes
    public Listed classes =
        new Listed(SomeTest.class, YetAnotherTest.class);

    @RunnerFilter
    public CategoryFilter filter = CategoryFilter.include(Yes.class);
}
{% endhighlight %}

### Putting the pieces together

So what? Well, instead of specifying the classes explicitly you could employ the capabilities of the `ClasspathSuite` to determine the test classes dynamically. For this purpose, I have written a small wrapper around Johannes Links' `ClasspathSuite`. The above example can thus be rewritten without explicitly specifying the test classes:

{% highlight java %}
@RunWith(SuiteBuilder.class)
public class OnlyYes {
    @Classes
    public InClasspath classes = new InClasspath();

    @RunnerFilter
    public CategoryFilter filter = CategoryFilter.include(Yes.class);
}
{% endhighlight %}

The wrapper offers the same flexibility as the `ClasspathSuite`, e.g.:

{% highlight java %}
@RunWith(SuiteBuilder.class)
public class OnlyYes {
    @Classes
    public InClasspath classes = new InClasspath().includingJars()
            .filteredBy(".*Test", "!.*AllTests")
            .includingOnlySubclassesOf(MyBaseTest.class);

    @RunnerFilter
    public CategoryFilter filter = CategoryFilter.include(Yes.class);
}
{% endhighlight %}

While I will look into how this can be integrated into JUnit or ClasspathSuite feel free to contact me if you are interested in the source code of the `InClasspath` class.

### Update

I am currently working on integrating ClasspathSuite and InClasspath into core JUnit... In the meantime, you can [take a look at the code on GitHub](http://github.com/marcphilipp/junit/tree/master/src/main/java/org/junit/experimental/cpsuite/).
