# Contribution guide

Thank you for your interest in contributing to Kotlin!

This guide explains the contribution process established in the Kotlin IntelliJ IDEA plugin, as well as defines requirements for the changes.
Please read the document carefully before submitting requests.

Here are links to other related projects you might want to contribute to:

- Kotlin compiler and runtime libraries: [JetBrains/kotlin](https://github.com/JetBrains/kotlin)
- IntelliJ IDEA Community: [JetBrains/intellij-community](https://github.com/JetBrains/intellij-community)

## Contents

1. [How to contribute](#1-how-to-contribute)
2. [What to contribute](#2-what-to-contribute)
3. [Pull request requirements](#3-pull-request-requirements)
4. [Writing intentions/inspections](#4-writing-intentionsinspections)

## 1. How to contribute

You can contribute to the plugin by sending us a Pull Request to the `master` branch.

Because of technical reasons, we can't use the Github UI for accepting PRs.  Instead, we pick commits manually. Don't worry: 
we won't change the commits (except maybe trivial changes such as typos) or its author information.

You will likely need to set up a project locally to modify its code.
The detailed setup instructions are available in the [README](README.md).

To learn how to write plugins for IntelliJ IDEA, please refer to the 
[IntelliJ Platform SDK DevGuide](https://jetbrains.org/intellij/sdk/docs/intro/welcome.html). 
The portal contains a lot of useful information and explains how the IDE works under-the-hood.

There's also a "#contributors" channel in the [public Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up). 
Please feel free to ask questions regarding contributions to the Kotlin IntelliJ IDEA plugin.

## 2. What to contribute

A excellent way to contribute would be to fix some of the [issues marked with the "up-for-grabs" tag](https://youtrack.jetbrains.com/issues/KT?q=tag:%20%7BUp%20For%20Grabs%7D%20and%20(State:%20Open%20or%20State:%20Backlog)%20and%20(Subsystems:%20IDE*)).
They usually don't take a lot of time to be fixed, and the fix is often local.

If you plan to work on new functionality (new intentions, inspections and so on) or want to fix something by re-writing a significant
part of a subsystem, please contact us beforehand via the "#contributors" channel on Slack. Our experience shows that features that seem
trivial often require thorough design work, or implementing them might dramatically affect the performance of the whole IDE.
So we might not be able to accept the Pull Request, even if it brings significant value to the users.

Surely, this doesn't mean we say no to arbitrary changes. However, the review process might be slightly longer than usual. So sticking
to the "up-for-grabs" tag is a safe way to go.

## 3. Pull Request requirements

We have several requirements for the Pull Requests:

1. A Pull Request must solve some issue. So the issue identifier must be specified.  
   We use [KT](https://youtrack.jetbrains.com/issues/KT) and [KTIJ](https://youtrack.jetbrains.com/issues/KTIJ) projects on YouTrack.
   If the issue doesn't exist yet, please create it in [KTIJ](https://youtrack.jetbrains.com/issues/KTIJ).
2. Do not submit Pull Requests that solve multiple issues that aren't interconnected. Create several PRs instead.
3. The plugin must still work after the change. Please ensure the plugin compiles and runs successfully after the change.
4. Each non-trivial change in plugin functionality must come with tests, so add new tests or adapt existing ones. 
   In both cases, please ensure the existing tests don't fail with the PR applied.
5. Avoid merge commits. If you want to adapt your PR after changes in `master`, use `git pull --rebase` instead.

## 4. Writing intentions/inspections

This section contains tips that might help you to write a 

### 4.1. IntelliJ IDEA

You can find necessary information about how to write inspections, intentions and quick fixes 
in the [Code Inspections](https://jetbrains.org/intellij/sdk/docs/tutorials/code_inspections.html) tutorial.

It's essential to know 
about [PSI](https://www.jetbrains.org/intellij/sdk/docs/basics/architectural_overview/psi.html), the source code model used in IntelliJ IDEA.
To inspect PSI, you can use either the built-in [Psi Viewer](https://www.jetbrains.com/help/idea/psi-viewer.html) available 
in the "internal" mode or an external plugin called [PsiViewer](https://plugins.jetbrains.com/plugin/227-psiviewer).

### 4.2. Kotlin-specific information

#### General information

1. Intentions and inspections must be registered in [`idea/resources/META-INF/inspections.xml`](idea/resources/META-INF/inspections.xml).
2. There must be a description in English.
   Put descriptions for intentions to [`idea/resources-en/intentionDescriptions`](idea/resources-en/intentionDescriptions),
   descriptions for inspections are in [`idea/resources-en/inspectionDescriptions`](idea/resources-en/inspectionDescriptions).
3. All intentions, inspections and quick-fixes should have automatic tests.
   Put test data for local inspections with quick fixes to [`idea/testData/inspectionsLocal`](idea/testData/inspectionsLocal),
   inspections without quick fixes to [`idea/testData/inspections`](idea/testData/inspections).
4. Prefer inspections with `ProblemHighlightType.INFORMATION` and attached quick-fixes over intentions.
5. In inspection code, use only `ProblemHighlightType.GENERIC_ERROR_OR_WARNING` as it allows users to adjust 
   the inspection level by themselves.
6. Make the inspection highlighting range as narrow as possible. E.g. highlight only the class name instead of the whole class declaration,
   and a called function name instead of the whole call.
7. Never hard-code strings displayed to the user in code. Use language bundles instead.
   See the [`KotlinBundle`](idea/resources-en/messages/KotlinBundle.properties) as an example.
8. You can test the exception on the kotlin-ide project itself. To do it, clone kotlin-ide to some other directory 
   (or create a Git worktree), run the "IDEA" run configuration, open the cloned project in it and run your inspection using
   the "Run Inspection by Name" action.  
   Here's a short check list:
   - [ ] Check that you didn't introduce performance issues by monitoring execution time;
   - [ ] Check if memory consumption is reasonable;
   - [ ] Check false positives (or maybe false negatives);
   - [ ] Try to apply quick fixes and check if code is correct.

#### Reference resolution

1. Resolution operations (`analyze()`, `resolveToCall()`, `resolveToDescriptors()` etc.) are expensive. Perform checks that don't depend on
   resolution first. For instance, if your inspection looks for top-level classes annotated with `@Foo`, check if the current declaration is
   a top-level class before resolving the `Foo` reference.
2. Avoid using `resolveToDesciptor()` unless necessary as it throws an exception if the reference can't be resolved.
   Use `resolveToDescriptorIfAny()` instead.
3. If you need to resolve several references at once, call `analyze()` on a common `PsiElement` and then fetch results for individual
   references from the received `BindingContext`. Calling `resolveToDescriptorIfAny()` or `resolveToCall()` several times may lead to
   inconsistent results.
4. Don't depend too much on applicability checks. Code could have changed since their execution. Check the required pre-conditions once
   more before code modification.
5. Use the `languageVersionSettings` extension property to get `LanguageVersionSettings` for a particular `PsiElement`.

#### Performance and stability tips

1. Never do anything continuous in the UI thread. Never call `resolve()` from it.
2. Always start potentially long operations (such as "Usages search") under a `ProgressIndicator` so the user can cancel it if needed.
3. Access PSI, module and project components only from a read/write action.
4. Use `SmartPsiElementPointer` to store references to `PsiElement`s across multiple read/write actions. IntelliJ IDEA might dispose or
   replace a `PsiElement` between the actions.
5. Do not store any references to PSI or a project structure statically or in long-living project components. It will surely lead
   to memory leaks. If absolutely necessary, use `SmartPsiElementPointer` (and corresponding `createSmartPointer()`).