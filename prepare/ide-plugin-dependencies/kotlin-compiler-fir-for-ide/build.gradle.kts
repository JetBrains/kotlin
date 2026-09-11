plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val firCompilerModules: Array<String> = CompilerModules.firCompilerModules
val jvmCompilerModules: Array<String> = CompilerModules.jvmCompilerModules

val excludedFirModules = listOf(
    ":compiler:fir:raw-fir:light-tree2fir",
    ":compiler:multiplatform-parsing",
)

val additionalK1Modules = listOf(
    ":core:deserialization",
    ":core:descriptors.jvm",
)

val projects = firCompilerModules.asList() + jvmCompilerModules + additionalK1Modules - excludedFirModules

publishJarsForIde(projects)
