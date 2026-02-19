plugins {
    kotlin("jvm")
}

val commonCompilerModules: Array<String> by rootProject.extra

val excludedCompilerModules = listOf(
    // Sic! Includes ":compiler:cli-base" as there are many non-CLI clients depending on compiler options
    ":compiler:cli",
    ":compiler:cli-jvm",
    ":compiler:cli-js",
    ":compiler:cli-metadata",
    ":compiler:javac-wrapper",
    ":compiler:incremental-compilation-impl"
)

val projects = commonCompilerModules.asList() - excludedCompilerModules + listOf(
    ":kotlin-compiler-runner-unshaded",
    ":kotlin-preloader",
    ":daemon-common",
    ":kotlin-daemon-client"
)

publishJarsForIde(
    projects = projects,
    libraryDependencies = listOf(protobufFull())
)
