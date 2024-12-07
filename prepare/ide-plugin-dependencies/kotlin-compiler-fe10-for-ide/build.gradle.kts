plugins {
    kotlin("jvm")
}

val fe10CompilerModules: Array<String> by rootProject.extra

val excludedCompilerModules = listOf(
    ":compiler:cli",
    ":compiler:cli-js",
    ":compiler:javac-wrapper",
    ":compiler:incremental-compilation-impl"
)

val extraCompilerModules = listOf(
    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
)

val projects = fe10CompilerModules.asList() - excludedCompilerModules + extraCompilerModules

publishJarsForIde(projects)
