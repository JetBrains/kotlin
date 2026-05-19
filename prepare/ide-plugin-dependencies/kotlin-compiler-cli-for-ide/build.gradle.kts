plugins {
    kotlin("jvm")
}

val cliCompilerModules: Array<String> by rootProject.extra

val excludedCliCompilerModules = listOf(
    // These modules are included into kotlin-compiler-common-for-ide
    ":compiler:arguments.common",
    ":compiler:cli-base",
)

val projects = cliCompilerModules.asList() - excludedCliCompilerModules

publishJarsForIde(projects)
