/*
  This artifact is deprecated should be used only for compatibility reasons for existing IDE branches like kt-211-master, kt-203-master
  kotlin-compiler-for-ide was split into multiple jars: kotlin-compiler-fe10-for-ide, kotlin-compiler-common-for-ide, kotlin-compiler-fir-for-ide, etc
  For kt-212-master IDE branch and newer split compiler jars should be used:
 */

plugins {
    kotlin("jvm")
}

val compilerModules: Array<String> by rootProject.extra

val excludedCompilerModules = listOf(
    ":compiler:cli",
    ":compiler:cli-js",
    ":compiler:javac-wrapper",
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":js:js.dce",
    ":compiler:ir.serialization.web",
    ":compiler:incremental-compilation-impl",
    ":compiler:fir:raw-fir:light-tree2fir"
)

val projects = compilerModules.asList() - excludedCompilerModules + listOf(
    ":kotlin-compiler-runner-unshaded",
    ":kotlin-preloader",
    ":daemon-common",
    ":kotlin-daemon-client"
)

publishJarsForIde(
    projects = projects,
    libraryDependencies = listOf(protobufFull())
)
