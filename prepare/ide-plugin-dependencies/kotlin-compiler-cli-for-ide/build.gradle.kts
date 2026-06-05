plugins {
    kotlin("jvm")
}

@Suppress("UNCHECKED_CAST")
    val cliCompilerModules = rootProject.extra["cliCompilerModules"] as Array<String>

val excludedCliCompilerModules = listOf(
    // These modules are included into kotlin-compiler-common-for-ide
    ":compiler:arguments.common",
    ":compiler:cli-base",
)

val projects = cliCompilerModules.asList() - excludedCliCompilerModules

publishJarsForIde(projects)
