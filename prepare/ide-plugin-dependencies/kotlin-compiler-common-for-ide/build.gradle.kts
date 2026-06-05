plugins {
    kotlin("jvm")
}

@Suppress("UNCHECKED_CAST")
    val commonCompilerModules = rootProject.extra["commonCompilerModules"] as Array<String>
@Suppress("UNCHECKED_CAST")
    val analysisApiModules = rootProject.extra["analysisApiModules"] as Array<String>

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
