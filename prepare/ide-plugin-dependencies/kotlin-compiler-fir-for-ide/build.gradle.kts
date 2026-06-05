plugins {
    kotlin("jvm")
}

@Suppress("UNCHECKED_CAST")
    val firCompilerModules = rootProject.extra["firCompilerModules"] as Array<String>

val excludedFirModules = listOf(
    ":compiler:fir:raw-fir:light-tree2fir",
)

val projects = firCompilerModules.asList() - excludedFirModules

publishJarsForIde(projects)
