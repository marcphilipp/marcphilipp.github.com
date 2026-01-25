---
title: "STF Milestone 10: Documentation improvements"
date: 2026-01-25 00:01
categories: [Sovereign Tech Fund, JUnit]
lang: en
ref: 2026-01-25-stf-milestone-10-documentation-improvements
note: |
  This post is part of the series on my work on JUnit supported by the [Sovereign Tech Fund](https://www.sovereign.tech/programs/fund) (STF). Please refer to the [initial post](/blog/2025/01/19/being-a-full-time-open-source-maintainer-supported-by-the-sovereign-tech-fund/) for context and a list of all posts.
---

JUnit has long been publishing its User Guide and API documentation for every release on [junit.org](https://junit.org).
However, there was no way to easily navigate between versions and no warnings when browsing outdated or preview releases.
In [this milestone](https://github.com/junit-team/junit-framework/issues/5244), I finally had the chance to resolve these long-standing issues.
<!--more-->
{:.lead}

Prior to this milestone, the only way of accessing a specific version was by manually changing the URL.
For example, the User Guide for version 5.13.0 is available at [https://docs.junit.org/5.13.0/user-guide/](https://docs.junit.org/5.13.0/user-guide/).
Manually changing `5.13.0` in the URL to `5.14.0` takes one to the corresponding version.
However, doing so requires knowledge about which versions are available and is inconvenient and error-prone.

Moreover, users should view the documentation specific to the version they're using.
Looking at documentation that is older or more recent can be confusing: it can point out features that have better replacements or don't exist yet in a stable version.
Therefore, we have decided to adopt [Antora](https://antora.org/) which – among other things – adds a version selector to each documentation page.
In addition, since it's aware of all versions being deployed as part of the documentation site, it provides the ability to render a banner on [outdated](https://docs.junit.org/6.0.1/) or [preview](https://docs.junit.org/6.1.0-M1/) releases.
Rather than having a single HTML page for each version, Antora encourages splitting the documentation into separate pages that are focussed on a single topic.

I started working on this milestone after we had released version 6.0.0.
I really wanted 6.0.0 to be part of the new documentation site, though.
Similarly, since we will continue to support 5.14.x for some time, at least the latest 5.14.x release should also be available on the Antora site.
Therefore, I decided to write [a script](https://github.com/marcphilipp/antora-migrator) that would migrate our Asciidoctor-based documentation to an Antora component so that I could run it on different Git branches for different releases.
This strategy worked out nicely in the end!

Another challenge was adopting Antora's support for the latest release and prerelease versions.
We wanted to ensure that the those versions continued to be available at [https://docs.junit.org/current/](https://docs.junit.org/current/) and [https://docs.junit.org/snapshot/](https://docs.junit.org/snapshot/), respectively.
Before adopting Antora, we had achieved that by publishing the latest release in two places: `/current` and `/6.0.2`, for example.
Instead of duplicating pages, Antora relies on redirects.
However, GitHub Pages – where the site had been hosted until now – does not support redirects.
After trying a few alternatives, we decided to migrate to [statichost.eu](https://statichost.eu) for hosting instead.
It was very simple to set up and connect to GitHub.
It being privacy-focused and hosted in the EU was a nice bonus.

Overall, I'm very happy how it turned out!
Antora provides a solid foundation to build on.
I'm sure the few limitations we encountered (e.g. its search functionality) will be improved over time.
But don't take my word for it and have a look at [docs.junit.org](https://docs.junit.org) for yourself!

[![screenshot of Antora-based documentation site]({{ "/img/posts/2026-01-25-stf-milestone-10-documentation-improvements/docs-site-screenshot.png" | prepend: site.baseurl }}){: .img-responsive .img-thumbnail }](https://docs.junit.org)
