plugins {
    kotlin("jvm")
}

val firCompilerModules: Array<String> by rootProject.extra

val excludedFirModules = listOf(
    ":compiler:fir:raw-fir:light-tree2fir",
)

val projects = firCompilerModules.asList() - excludedFirModules

publishJarsForIde(projects)
