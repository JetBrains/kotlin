plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val firCommonCompilerModules: Array<String> = CompilerModules.firCommonCompilerModules
val jvmCompilerModules: Array<String> = CompilerModules.jvmCompilerModules

val additionalK1Modules = listOf(
    ":core:deserialization",
    ":core:descriptors.jvm",
)

val projects = firCommonCompilerModules.asList() + jvmCompilerModules + additionalK1Modules

publishJarsForIde(projects)
