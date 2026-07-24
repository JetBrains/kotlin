plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val fe10CompilerModules: Array<String> = CompilerModules.fe10CompilerModules
val jvmCompilerModules: Array<String> = CompilerModules.jvmCompilerModules

val excludedCompilerModules = listOf(
    ":compiler:javac-wrapper",
    ":compiler:incremental-compilation-impl",
)

val extraCompilerModules = listOf(
    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
    ":compiler:frontend.java",
)

val projects = fe10CompilerModules.asList() - excludedCompilerModules + jvmCompilerModules + extraCompilerModules

publishJarsForIde(projects)
