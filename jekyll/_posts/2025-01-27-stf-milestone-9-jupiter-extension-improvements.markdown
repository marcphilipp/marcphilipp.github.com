---
title: "STF Milestone 9: Jupiter extension improvements"
date: 2025-01-27 00:01
categories: [Sovereign Tech Fund, JUnit 5]
lang: en
ref: stf-milestone-9-jupiter-extension-improvements
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

As you can tell from the high number, this milestone was originally planned to be done later. However, a contributor ([@JojOatXGME](https://github.com/JojOatXGME)) was eager to work on it so I reached out to the STF and asked to change the order. They agreed and we changed the plan accordingly.<!--more-->
{: .lead}

The [issue addressed by this milestone](https://github.com/junit-team/junit5/issues/4057) was a crucial piece of missing functionality in the Jupiter extension API that kept popping up and was re-discussed every few weeks/months: there was no way to access the method-level extension context for `TestInstancePostProcessor` (and other) extensions which is important to create and destroy resources on the test class instance level. The hope was that resolving this issue would reduce friction for users and free up time for maintainers to work on other duties.

After the [initial PR](https://github.com/junit-team/junit5/pull/4032) from Johannes that introduced a new annotation to opt-in to the new behavior, I iterated quite a bit on it based on feedback from the rest of the JUnit team. We decided to introduce a new interface with a method instead because it allows custom logic to determine which scope should be used (for example, checking for a configuration parameter):

```java
public interface TestInstantiationAwareExtension extends Extension {
    default ExtensionContextScope getTestInstantiationExtensionContextScope(ExtensionContext ctx) {
       return ExtensionContextScope.DEFAULT;
    }
    enum ExtensionContextScope {  
       DEFAULT, TEST_METHOD
    }
}
```

I then added a configuration parameter to change the default behavior so it could easily be tested by consuming projects without editing any code. Moreover, I updated the [User Guide](https://junit.org/junit5/docs/snapshot/user-guide/#extensions-test-instance-post-processing) and modified all core extensions to opt-in right away. This change introduced constructor injection support for `@TempDir` and now allows writing a test class that requires a temporary directory like this (using a `record` for brevity):

```java
record TempDirTests(@TempDir Path tempDir) {
    @Test
    void shouldExists() {
       assertTrue(Files.exists(tempDir));
    }
    @Test
    void shouldAllowCreatingFiles() throws IOException {
       Files.createFile(tempDir.resolve("test.txt"));
    }
}
```

Summing up, I was grateful that the STF allowed us the flexibility to change the original plan. Even more importantly, I was really happy that this long-standing issue was finally resolved!
