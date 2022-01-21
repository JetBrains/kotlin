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

val projects = fe10CompilerModules.asList() - excludedCompilerModules

publishJarsForIde(projects)
