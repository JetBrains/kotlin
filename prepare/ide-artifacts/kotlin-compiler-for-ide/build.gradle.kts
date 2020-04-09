val compilerModules: Array<String> by rootProject.extra

val excludedCompilerModules = listOf(
    ":compiler:cli-js-klib",
    ":compiler:javac-wrapper",
    ":compiler:backend.js",
    ":compiler:backend.wasm",
    ":js:js.dce",
    ":compiler:ir.serialization.js",
    ":compiler:incremental-compilation-impl",
    ":compiler:fir:raw-fir:light-tree2fir"
)

val projects = compilerModules.asList() - excludedCompilerModules + listOf(
    ":kotlin-compiler-runner",
    ":kotlin-preloader",
    ":daemon-common",
    ":daemon-common-new",
    ":kotlin-daemon",
    ":kotlin-daemon-client",
    ":kotlin-daemon-client-new"
)

publishProjectJars(
    projects = projects,
    libraryDependencies = listOf(protobufFull())
)