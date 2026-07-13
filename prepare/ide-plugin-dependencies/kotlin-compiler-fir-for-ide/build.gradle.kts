plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val firCompilerModules: Array<String> = CompilerModules.firCompilerModules

val excludedFirModules = listOf(
    ":compiler:fir:raw-fir:light-tree2fir",
)

val projects = firCompilerModules.asList() - excludedFirModules

publishJarsForIde(projects)
