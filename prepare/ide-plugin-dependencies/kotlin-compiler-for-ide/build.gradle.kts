idePluginDependency {
    val compilerModules: Array<String> by rootProject.extra

    val excludedCompilerModules = listOf(
        ":compiler:cli",
        ":compiler:cli-js",
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
        ":kotlin-daemon-client"
    )

    publishProjectJars(
        projects = projects,
        libraryDependencies = listOf(protobufFull())
    )
}
