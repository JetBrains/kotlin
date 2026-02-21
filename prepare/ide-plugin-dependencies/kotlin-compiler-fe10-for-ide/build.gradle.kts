plugins {
    id("root-config")
    kotlin("jvm")
}

val fe10CompilerModules = ProjectModuleLists.fe10CompilerModules

val excludedCompilerModules = listOf(
    ":compiler:javac-wrapper",
    ":compiler:incremental-compilation-impl"
)

val extraCompilerModules = listOf(
    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
)

val projects = fe10CompilerModules.asList() - excludedCompilerModules + extraCompilerModules

publishJarsForIde(projects)
