---
title: "JUnit 5 auf der Devoxx US 2017"
date: 2017-04-21 16:12
comments: true
categories: [Talks, JUnit 5]
lang: de
ref: post-junit5-at-devoxx-us-2017
---

Vor ein paar Wochen habe ich an der Konferenz [Devoxx US](http://cfp.devoxx.us/2017/talk/ZCD-4979/JUnit_5_-_The_New_Testing_Framework_for_Java_and_Platform_for_the_JVM) in San Jose, CA, teilgenommen und einen Vortrag über [JUnit 5](http://junit.org/junit5/) gehalten. Die Aufzeichnung (auf Englisch) ist nun auf YouTube verfügbar.<!--more-->
{: .lead}

<div class="embed-responsive embed-responsive-16by9">
  <iframe src="https://www.youtube-nocookie.com/embed/0qI6_NKFQsY?rel=0" frameborder="0" allowfullscreen></iframe>
</div>

## Code-Beispiele

Den Beispielcode gibt's auf [GitHub](https://github.com/marcphilipp/junit5-demo/tree/20170323-devoxx.us/src/test/java/com/example).

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

## Folien

Die Folien (auf Englisch) kann man auf [SpeakerDeck](https://speakerdeck.com/marcphilipp/junit-5-the-new-testing-framework-for-java-and-platform-for-the-jvm) anschauen.

<script async class="speakerdeck-embed" data-id="7f3a63c8ecbb4bd98f4878fab2e07b09" data-ratio="1.77777777777778" src="//speakerdeck.com/assets/embed.js"></script>
