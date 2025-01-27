---
title: "STF Milestone 1: Adopt Renovate"
date: 2025-01-19 00:01
categories: [Sovereign Tech Fund, JUnit 5]
lang: en
ref: stf-milestone-1-adopt-renovate
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

When drafting the project description for the STF, it was important to choose something as a first milestone that I could get done in under a month and that would have a direct benefit to the project right away and improve its maintainability. That's why I eventually decided on migrating to Renovate for automating updates to dependencies and build tools.<!--more-->
{: .lead}

We had already come a long way as a team when it comes to managing dependency updates. When we started to work on JUnit 5 in 2015, dependencies were few and we updated them manually. Over time, we adopted more tools, in particular in the build. We used the [Gradle Versions Plugin](https://github.com/ben-manes/gradle-versions-plugin) for a while but it had its limitations. Eventually, we adopted [Dependabot](https://docs.github.com/en/code-security/dependabot) and switched to Gradle's [version catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html) which made updating our main dependencies much easier.

However, the amount of builds we needed to manage increased over time. Standalone projects such as the [TestNG engine](https://github.com/junit-team/testng-engine) were introduced and [samples](https://github.com/junit-team/junit5-samples) for more build tools were added. Dependabot only supported a subset of those. It also did not support keeping the build tools themselves up to date. So we had a [separate repository](https://github.com/junit-team/wrapper-upgrade) that used the [Wrapper Upgrade Gradle Plugin](https://github.com/gradle/wrapper-upgrade-gradle-plugin/) for updating our Gradle and Maven (for samples) builds.

Adopting [Renovate](https://mend.io/renovate) promised to enable us to remove this incomplete patchwork of tools with a single solution. So far, it has lived up to this promise. It not only supports Gradle and Maven, but also Bazel, sbt, and npm. We now also use it for pinning our dependencies in GitHub Action workflows. It allowed us to remove the separate repo for updating wrappers and gives us peace of mind that everything is shipshape regarding our dependencies.

The flip side of the coin are many PRs that can be quite noisy at times. While Renovate can be configured to work without PRs, we decided against that after discussing the topic in the JUnit team. We also decided to enable its auto-merge feature but require its PRs to be approved prior to merging to reduce the risk of supply chain attacks. Overall, I think we're happy with the switch and would do it again.
