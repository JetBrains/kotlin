plugins {
    kotlin("jvm")
}

val commonCompilerModules: Array<String> by rootProject.extra
val analysisApiModules: Array<String> by rootProject.extra

val excludedAnalysisApiModules = listOf(
    ":analysis:decompiled",
)

val projects = commonCompilerModules.asList() + analysisApiModules - excludedAnalysisApiModules + listOf(
    ":compiler:arguments.common",
    ":compiler:cli-base",
    ":kotlin-build-common",
    ":kotlin-compiler-runner-unshaded",
    ":kotlin-preloader",
    ":daemon-common",
    ":kotlin-daemon-client",
    ":compiler:build-tools:kotlin-build-tools-api",
)

publishJarsForIde(
    projects = projects,
    libraryDependencies = listOf(protobufFull())
)
