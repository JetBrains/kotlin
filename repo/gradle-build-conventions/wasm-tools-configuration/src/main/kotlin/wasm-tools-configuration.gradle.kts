import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.build.wasmtools.WasmToolsExtension

/*
 * Provisions the `wasm-tools` CLI, which is used to turn Kotlin/Wasm WASI core modules into WASI 0.2 components
 * (see KT-87723). Distributions come from the `bytecodealliance/wasm-tools` GitHub releases via cache-redirector,
 * see `wasmToolsDistributions()`.
 *
 * Applying this plugin adds the `wasmToolsKotlinBuild` extension:
 *  - `Test.setupWasmTools()` passes the executable to a test JVM as the `wasm.tools.path` system property,
 *  - `Task.useWasmTools()` returns the executable path for other tasks (query it at execution time only).
 *
 * `-Pwasm.tools.path=/path/to/wasm-tools` overrides the provisioned distribution, e.g. for local experiments.
 */

val wasmToolsVersion = WasmToolsExtension.version(project)

val classifier = run {
    val os = OperatingSystem.current()
    val isArm64 = System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64")
    when {
        os.isLinux -> if (isArm64) "aarch64-linux" else "x86_64-linux"
        os.isMacOsX -> if (isArm64) "aarch64-macos" else "x86_64-macos"
        os.isWindows -> if (isArm64) "aarch64-windows" else "x86_64-windows"
        else -> error("wasm-tools is not available for $os")
    }
}
val archiveExtension = if (OperatingSystem.current().isWindows) "zip" else "tar.gz"

val wasmTools = configurations.create("wasmTools") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    add(wasmTools.name, "bytecodealliance.wasm-tools:wasm-tools:$wasmToolsVersion:$classifier@$archiveExtension")

    // so that the other platforms are covered by dependency locking/verification as well
    for (otherClassifier in listOf("x86_64-linux", "aarch64-linux", "x86_64-macos", "aarch64-macos")) {
        add("implicitDependencies", "bytecodealliance.wasm-tools:wasm-tools:$wasmToolsVersion:$otherClassifier@tar.gz")
    }
    for (otherClassifier in listOf("x86_64-windows", "aarch64-windows")) {
        add("implicitDependencies", "bytecodealliance.wasm-tools:wasm-tools:$wasmToolsVersion:$otherClassifier@zip")
    }
}

val distributionName = "wasm-tools-$wasmToolsVersion-$classifier"
val installationDirectory = layout.buildDirectory.dir("tools/wasm-tools")
val executableName = if (OperatingSystem.current().isWindows) "wasm-tools.exe" else "wasm-tools"

val unpackWasmTools = tasks.register<Sync>("unpackWasmTools") {
    description = "Download and unpack the wasm-tools CLI"
    group = "wasm-tools"

    from({
        if (archiveExtension == "zip") zipTree(wasmTools.singleFile) else tarTree(wasmTools.singleFile)
    }) {
        // the archives contain a single `<distributionName>/` directory
        eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
        includeEmptyDirs = false
    }
    into(installationDirectory.map { it.dir(distributionName) })

    val executable = installationDirectory.map { it.dir(distributionName).file(executableName) }
    doLast {
        executable.get().asFile.setExecutable(true)
    }
}

val wasmToolsExecutablePath: Provider<String> =
    providers.gradleProperty("wasm.tools.path").orElse(
        unpackWasmTools.map { it.destinationDir.resolve(executableName).absolutePath }
    )

extensions.create<WasmToolsExtension>(
    "wasmToolsKotlinBuild",
    project,
    unpackWasmTools,
    wasmToolsExecutablePath,
)
