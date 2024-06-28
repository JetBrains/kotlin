# IDE Plugin Dependencies Validator

Some projects inside the Kotlin repository are used inside the IntelliJ Kotlin plugin. Those projects have special restrictions forbidding
experimental Kotlin stdlib API use in them. The `:tools:ide-plugin-dependencies-validator` project is a tool to check that those
restrictions are not violated.

See [KTIJ-20529](https://youtrack.jetbrains.com/issue/KTIJ-20529).

## Details

IntelliJ IDEA has its own bundled Kotlin stdlib, which is used across the IntelliJ repository. This stdlib is usually the latest available
stable Kotlin stdlib. Kotlin repository is compiled against a snapshot Kotlin stdlib. So, the projects from the Kotlin repository,
which bundle to IntelliJ, are compiled against snapshot stdlib, but on the runtime in IntelliJ, there is a fixed and stable version of
Kotlin stdlib. To avoid binary compatibility problems that may arise at runtime in IntelliJ, experimental stdlib declarations for which
binary compatibility is not guaranteed are not allowed to be used inside Kotlin projects used in IntelliJ.

The list of such projects used inside IntelliJ Kotlin Plugin can be found at `projectsUsedInIntelliJKotlinPlugin` property
inside [the root build.gradle.kts](build.gradle.kts)

## Checks

The tool checks on all projects defined in `projectsUsedInIntelliJKotlinPlugin`.

* No opt-ins for experimental stdlib annotations are used inside the source code. The check works by syntax only, and no code resolution is
  performed. This is not a hundred percent reliable, but it usually works unless there is a name conflict with experimental opt-in
  annotations
  from stdlib. The code for those checks can be found at `org.jetbrains.Kotlin.ide.plugin.dependencies.validator` package.
* The Kotlin API version used is the same as the version of Kotlin stdlib used inside the IntelliJ Platform. This is defined
  by `kotlinApiVersionForProjectsUsedInIntelliJKotlinPlugin` property.
* No opt-ins for experimental stdlib annotations are used inside the build definition files. The check works by
  checking that no `-opt-in` arguments with experimental stdlib annotations are defined
  in `KotlinJvmCompile.KotlinOptions.freeCompilerArgs`.

## Running Checks

`gradle :tools:ide-plugin-dependencies-validator:checkIdeDependenciesConfiguration` task runs the check. It consists of two subtasks:

* `gradle :tools:ide-plugin-dependencies-validator:checkIdeDependenciesConfiguration` task checks the build configuration of projects.
* `gradle :tools:ide-plugin-dependencies-validator:run` task checks Kotlin source files inside the project for experimental annotations
  usages.


