plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val cliCompilerModules: Array<String> = CompilerModules.cliCompilerModules

val excludedCliCompilerModules = listOf(
    // These modules are included into kotlin-compiler-common-for-ide
    ":compiler:arguments.common",
    ":compiler:cli-base",
)

val projects = cliCompilerModules.asList() - excludedCliCompilerModules

publishJarsForIde(projects)
