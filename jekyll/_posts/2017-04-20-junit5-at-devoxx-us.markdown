---
title: "JUnit 5 at Devoxx US 2017"
date: 2017-04-20 10:58
comments: true
categories: [Talks, JUnit 5]
lang: en
ref: post-junit5-at-devoxx-us-2017
---

A few weeks ago, I attended [Devoxx US](http://cfp.devoxx.us/2017/talk/ZCD-4979/JUnit_5_-_The_New_Testing_Framework_for_Java_and_Platform_for_the_JVM) in San Jose, CA, and gave a talk about [JUnit 5](http://junit.org/junit5/). The recording is now available on YouTube.<!--more-->
{: .lead}

<div class="embed-responsive embed-responsive-16by9">
  <iframe src="https://www.youtube-nocookie.com/embed/0qI6_NKFQsY?rel=0" frameborder="0" allowfullscreen></iframe>
</div>

## Abstract

> Over the last decade a lot has happened in the world of Java and testing, but JUnit 4 hasn't kept up. Now JUnit 5 is here to help shape the future of testing on the JVM with a focus on Java 8 language features, extensibility, and a modern programming API for testing in Java. Moreover, JUnit isn't just a Java testing framework anymore. Third parties are already developing test engines for Scala, Groovy, Kotlin, etc. that run on the new JUnit Platform.
>
> In this session, we will start off with an overview of the inspiration for and architecture of JUnit 5, from launchers to test engines. Then, we will take an example-driven tour of the new Jupiter programming model. We will explore the Jupiter extension model, learn about the extension points it provides, and see how custom extensions for conditional tests, method parameter resolution, lifecycle callbacks etc. are authored and registered. To round off the session, we will discuss migration strategies and compatibility with JUnit 4 and look at the roadmap of what's still to come.

## Code Examples

The complete example code is available on [GitHub](https://github.com/marcphilipp/junit5-demo/tree/20170323-devoxx.us/src/test/java/com/example).

```java
class SimpleTest {

    @Test
    @DisplayName("1 + 1 = 2")
    void myFirstTest() {
        assertEquals(2, 1 + 1);
    }

    @Test
    @Disabled("for some reason")
    void anotherTest() {
        assertEquals(0, 1 + 1);
    }
}
```

## Slides

The slides are available on [SpeakerDeck](https://speakerdeck.com/marcphilipp/junit-5-the-new-testing-framework-for-java-and-platform-for-the-jvm).

<script async class="speakerdeck-embed" data-id="7f3a63c8ecbb4bd98f4878fab2e07b09" data-ratio="1.77777777777778" src="//speakerdeck.com/assets/embed.js"></script>
