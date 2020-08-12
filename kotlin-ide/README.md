# Kotlin plugin [![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Kotlin plugin is IntelliJ IDEA plugin for the [Kotlin programming language](https://kotlinlang.org/).

## Building

### Requirements
1. IntelliJ IDEA Community or Ultimate
2. Kotlin Plugin with at least 1.4.0-rc bundled

### Steps
1. Check out this repository:

    ```bash
    git clone https://github.com/JetBrains/intellij-kotlin.git
    ```

2. Check out IDEA community to the `intellij/` subdirectory:

    ```bash
    cd intellij-kotlin
    git clone https://github.com/JetBrains/intellij-community.git intellij
    ```

3. Follow the set up instructions in the [Community repository](https://github.com/JetBrains/intellij-community/blob/master/README.md).
   Note that you have to set up multiple JDKs and possibly attach `tools.jar` from the JDK distribution to some of them.

4. Now you should be able to open and build the project in IntelliJ IDEA. Use "IDEA" run configuration to run IDEA from sources.

## FAQ

Q. What will happen with Kotlin plugin sources in [original Kotlin repo](https://github.com/JetBrains/kotlin/)?  
A. Eventually source code of Kotlin plugin from the old repo will be removed.

Q. Why was Kotlin plugin moved to this repo?  
A. Kotlin plugin and compiler have different release cycles. Also, now it's easier to edit intellij-community sources along
with sources of Kotlin plugin.

Q. What will happen with my pull request which I submitted to original Kotlin repo?  
A. Firstly, we don't accept NEW pull requests to plugin part of Kotlin in original Kotlin repo starting from 19 Aug 2020. We are
processing all left PRs in the old repo and eventually commits from the old repo will be cherry-picked to this repo.
