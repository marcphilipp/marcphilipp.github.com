---
title: "STF Milestone 2: Open Test Reporting"
date: 2025-03-21 00:01
categories: [Sovereign Tech Fund, JUnit]
lang: en
ref: stf-milestone-2-open-test-reporting
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

When JUnit was first released, it did not provide a feature to write test reports in machine-readable format. Build tools like [Ant](https://ant.apache.org/manual/Tasks/junit.html) filled that gap and introduced an XML-based report. It was later adopted and sometimes extended by other Java-based build tools such as [Maven](https://maven.apache.org/surefire/maven-surefire-plugin/index.html) and [Gradle](https://docs.gradle.org/current/userguide/java_testing.html#test_reporting). CI tools like [Jenkins](https://plugins.jenkins.io/junit/) introduced support for parsing these XML reports in order to display test results for each CI run. That led to other ecosystems also adopting the format to benefit from existing tool support. However, given that the format was initially defined for JUnit 3, it’s focused on test classes and methods and does not support nested structures. Some tools defined their own extensions of the schema that added such support or additional attributes. However, there are various partially conflicting schema definitions and subtle differences what gets written to which XML attributes since a proper "standard" was never agreed upon.

### Open Test Reporting to the rescue

To address this situation, the JUnit team originally introduced the [Open Test Reporting format](https://github.com/ota4j-team/open-test-reporting?tab=readme-ov-file#xml-format-specification) in 2022 along with JUnit 5.9. It aims to replace the legacy XML format in a way that supports all features of JUnit 5. Moreover, it intends to support general testing concepts rather being tied to test classes and methods in order to appeal to other ecosystems as well. Additionally, it supports [extension schemas](https://github.com/ota4j-team/open-test-reporting#schema-extensions) for adding framework-specific information.

Establishing a new format will, of course, take some time. Testing frameworks need to be adjusted in order to write the new format which they will only do if there is significant interest in their user communities. Similarly, tools like CI servers will only add support for reading test results written in the new format once there is a considerable number of testing frameworks supporting it. Therefore, [milestone 2](https://github.com/junit-team/junit5/issues/4113) of my STF-supported work focused on making the new format production-ready and increasing its appeal by providing an API and command-line tool for converting from XML into a user-friendly, standalone HTML report.

<!--more-->

### Developing a new HTML report

It's been a long time since I've been involved in setting up a frontend project from scratch. Therefore, I had mixed feelings before starting on the development of the HTML report. However, it actually turned out to be a lot of fun to work on once the initial setup was in place. Compared to Java, the development loop of making a change and seeing it in action was insanely fast. For reference, I decided to use [Vue.js](https://vuejs.org/) with Typescript and [Tailwind CSS](https://tailwindcss.com/). In addition, there's a little Java code that injects the test results into the template that is produced by [Vite](https://vite.dev/) during the build process.

I introduced a [CLI command](https://github.com/ota4j-team/open-test-reporting#html-report) that takes a single or multiple XML files as input and produces a self-contained HTML file. The test tree is rendered on the left with a details view on the right. The color and icon of the top bar changes depending on the overall status: it's red if there was at least one failure and green otherwise.

![screenshot of successful test report]({{ "/img/posts/2025-03-21-stf-milestone-2-open-test-reporting/html-report-successful.png" | prepend: site.baseurl }}){: .img-responsive .img-thumbnail }

The screenshot also shows one other major new features that was added in this milestone: file attachments. In case of images they are rendered inline. This is useful for integration and UI tests. The screenshot shows a browser test that is part of Open Test Reporting's own test suite and verifies the HTML report gets rendered correctly. The [`TestReporter` API](https://junit.org/junit5/docs/5.12.1/api/org.junit.jupiter.api/org/junit/jupiter/api/TestReporter.html#publishFile(java.lang.String,org.junit.jupiter.api.extension.MediaType,org.junit.jupiter.api.function.ThrowingConsumer)) in JUnit (along with a [similar method](https://junit.org/junit5/docs/5.12.1/api/org.junit.jupiter.api/org/junit/jupiter/api/extension/ExtensionContext.html#publishFile(java.lang.String,org.junit.jupiter.api.extension.MediaType,org.junit.jupiter.api.function.ThrowingConsumer)) in `ExtensionContext` for extensions) allows attaching files during test execution.

Of course, there's also a dark theme:

![screenshot of failed test report]({{ "/img/posts/2025-03-21-stf-milestone-2-open-test-reporting/html-report-failed.png" | prepend: site.baseurl }}){: .img-responsive .img-thumbnail }

The format is [extensible via an SPI](https://github.com/ota4j-team/open-test-reporting#extending-the-html-report). For example, the "JUnit metadata" section from the screenshot above is contributed by a JUnit-specific extension. This allows other testing frameworks to render HTML sections if they write additional information to the XML reports.

### Seeing it in action

If you want to see it in action, please have a look at [JUnit's CI builds](https://github.com/junit-team/junit5/actions/workflows/main.yml?query=branch%3Amain+is%3Asuccess) which archive the reports. You can find them at the end of the summary page of every GitHub Actions workflow run. Unfortunately, GitHub does not (yet?) support browsing HTML files directly, so you'll have to download the zip file and extract it locally. It contains one HTML report per subproject.

![screenshot of GitHub Actions workflow run artifacts]({{ "/img/posts/2025-03-21-stf-milestone-2-open-test-reporting/github-artifacts.png" | prepend: site.baseurl }}){: .img-responsive }

Even better, you can (and should!) try it out in your own project. JUnit‘s User Guide documents how to enable writing the XML report. You can then use the [Open Test Reporting CLI](https://github.com/ota4j-team/open-test-reporting#html-report) to convert it to HTML.

```
java -jar open-test-reporting-cli-0.2.2.jar html-report \
	--output open-test-report.html \
	open-test-reporting.xml
```

### Where to go from here

Once I was more or less done with the implementation part of this milestone, I reached out to testing framework maintainers in other ecosystems. Some were hesitant or lacked the time to investigate but others were quite enthusiastic. For example, [PHPUnit](https://github.com/sebastianbergmann/phpunit/issues/6077#issuecomment-2613863770) right away implemented a proof-of-concept and drafted a concrete plan for adopting the Open Test Reporting format. Moreover, they plan to deprecate the "legacy" XML format in their next major version!

All in all, I am really happy with the outcome of this milestone! If you have any questions about any of the above, please raise an issue in the [Open Test Reporting](https://github.com/ota4j-team/open-test-reporting) repository (if it's about the format, CLI, etc.) or the [JUnit 5](https://github.com/junit-team/junit5/) repo (if it's JUnit-specific).

Happy testing!
