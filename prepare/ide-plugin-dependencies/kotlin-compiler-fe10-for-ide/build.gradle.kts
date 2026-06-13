plugins {
    kotlin("jvm")
}

val fe10CompilerModules: Array<String> by rootProject.extra
val jvmCompilerModules: Array<String> by rootProject.extra

val excludedCompilerModules = listOf(
    ":compiler:javac-wrapper",
    ":compiler:incremental-compilation-impl",
)

val extraCompilerModules = listOf(
    ":analysis:analysis-tools:deprecated-k1-frontend-internals-for-ide-generated",
    ":js:js.serializer",
    ":compiler:frontend.java",
)

val projects = fe10CompilerModules.asList() - excludedCompilerModules + jvmCompilerModules + extraCompilerModules

publishJarsForIde(projects)
