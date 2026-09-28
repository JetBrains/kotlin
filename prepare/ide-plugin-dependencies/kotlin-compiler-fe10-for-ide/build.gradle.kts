plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val fe10CompilerModules: Array<String> = CompilerModules.fe10CompilerModules

val excludedCompilerModules = listOf(
    ":compiler:incremental-compilation-impl",
    ":core:deserialization",
    ":core:descriptors.jvm",
)

val extraCompilerModules = listOf(
    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
    ":compiler:frontend.java",
)

val projects = fe10CompilerModules.asList() - excludedCompilerModules + extraCompilerModules

publishJarsForIde(projects)
