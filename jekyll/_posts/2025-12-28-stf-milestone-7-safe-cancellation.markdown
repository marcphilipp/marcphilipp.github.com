---
title: "STF Milestone 7: Safe cancellation"
date: 2025-12-28 00:01
categories: [Sovereign Tech Fund, JUnit]
lang: en
ref: 2025-08-21-stf-milestone-7-safe-cancellation
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

Prior to this milestone, the JUnit Platform provided no safe way to cancel test execution early, e.g. after the first test failed.
The only option was to forcibly terminate the JVM running the tests.
However, that also caused cleanup operations, such as deleting temporary files or stopping Docker containers used in tests, to be skipped.
Letting all tests run is often wasteful in terms of resources such as CPU and causes longer feedback cycles for developers.
Therefore, [this milestone](https://github.com/junit-team/junit-framework/issues/4725) was all about introducing a safe cancellation mechanism to the JUnit Platform.<!--more-->
{:.lead}

The Launcher API is the main entry point to the JUnit Platform for IDEs and build tools.
The new [`CancellationToken`](https://docs.junit.org/6.0.1/api/org.junit.platform.engine/org/junit/platform/engine/CancellationToken.html) API allows clients to request cancellation of a running test execution.
The `Launcher` interface already contained two methods for executing tests; one taking a `LauncherDiscoveryRequest` and another taking a `TestPlan`.
Rather than adding two additional overloads that take a `CancellationToken`, a new [`LauncherExecutionRequest`](https://docs.junit.org/6.0.1/api/org.junit.platform.launcher/org/junit/platform/launcher/LauncherExecutionRequest.html) class was introduced that encapsulates all parameters required for test execution, including an optional `CancellationToken`.
This design allows for future extensions of the test execution parameters without breaking existing clients.
A `LauncherExecutionRequest` can be created from a `LauncherDiscoveryRequest`, a `TestPlan`, or via `LauncherDiscoveryRequestBuilder.forExecution()`.

Using these new concepts, clients can implement "fail fast" behavior as follows (see [User Guide](https://docs.junit.org/6.0.1/advanced-topics/launcher-api.html#launcher-cancellation) for details):

```java
CancellationToken cancellationToken = CancellationToken.create();

TestExecutionListener failFastListener = new TestExecutionListener() {
    @Override
    public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
        if (result.getStatus() == FAILED) {
            cancellationToken.cancel();
        }
    }
};

LauncherExecutionRequest executionRequest = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectClass(MyTestClass.class))
        .forExecution()
        .cancellationToken(cancellationToken)
        .listeners(failFastListener)
        .build();

try (LauncherSession session = LauncherFactory.openSession()) {
    session.getLauncher().execute(executionRequest);
}
```

Cancelling tests relies on test engines checking and responding to the `CancellationToken`.
As a stop-gap solution, the `Launcher` also checks the token and cancels test execution when multiple test engines are present at runtime.

At the time of writing, the following test engines support cancellation:

* JUnit Jupiter
* JUnit Vintage
* JUnit Platform Suite
* [TestNG](https://github.com/junit-team/testng-engine)
* All `HierarchicalTestEngine` implementations such as [Spock](https://spockframework.org/) and [Cucumber](https://github.com/cucumber/cucumber-jvm/tree/main/cucumber-junit-platform-engine)

In addition to the changes in JUnit, I submitted a [pull request](https://github.com/apache/maven-surefire/pull/3155) to the Maven Surefire project.
It uses the new cancellation mechanism to implement support for Surefire's [`skipAfterFailureCount`](https://maven.apache.org/surefire/maven-surefire-plugin/examples/skip-after-failure.html) feature.
It was merged and released in Maven Surefire 3.5.4.
For Gradle, there's an [open issue](https://github.com/gradle/gradle/issues/34184) to implement similar support across all testing frameworks.

To try out the new safe cancellation mechanism, you need to use [JUnit 6.0.0](https://docs.junit.org/6.0.1/release-notes.html#v6.0.0) or later.
I am looking forward to your feedback!
