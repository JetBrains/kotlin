plugins {
    kotlin("jvm")
}

val commonCompilerModules: Array<String> by rootProject.extra

val excludedCompilerModules = listOf(
    ":compiler:cli",
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
