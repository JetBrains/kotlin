# About build-logic Module

This module aims to share common build logic for whole projects, previously we were using [buildSrc](https://docs.gradle.org/7.6/userguide/organizing_gradle_projects.html#sec:build_sources),
but for some reasons like "A change in buildSrc causes the whole project to become out-of-date", we are migrating to [composite builds](https://docs.gradle.org/7.6/userguide/composite_builds.html),
which avoids the side effects of buildSrc.

For more information, you can ref https://proandroiddev.com/stop-using-gradle-buildsrc-use-composite-builds-instead-3c38ac7a2ab3.