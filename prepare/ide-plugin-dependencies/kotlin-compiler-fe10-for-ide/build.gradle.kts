plugins {
    kotlin("jvm")
}

@Suppress("UNCHECKED_CAST")
    val fe10CompilerModules = rootProject.extra["fe10CompilerModules"] as Array<String>
@Suppress("UNCHECKED_CAST")
    val jvmCompilerModules = rootProject.extra["jvmCompilerModules"] as Array<String>

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
