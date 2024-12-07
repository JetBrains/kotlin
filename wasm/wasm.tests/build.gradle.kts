import org.gradle.internal.os.OperatingSystem
import java.net.URI
import com.github.gradle.node.npm.task.NpmTask
import java.nio.file.Files
import java.util.*

plugins {
    kotlin("jvm")
    id("jps-compatible")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
    id("binaryen-configuration")
    id("nodejs-configuration")
}

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
}

repositories {
    ivy {
        url = URI("https://archive.mozilla.org/pub/firefox/nightly/")
        patternLayout {
            artifact("2024/05/[revision]/[artifact]-[classifier].[ext]")
        }
        metadataSources { artifact() }
        content { includeModule("org.mozilla", "jsshell") }
    }
    ivy {
        url = URI("https://github.com/WasmEdge/WasmEdge/releases/download/")
        patternLayout {
            artifact("[revision]/WasmEdge-[revision]-[classifier].[ext]")
        }
        metadataSources { artifact() }
        content { includeModule("org.wasmedge", "wasmedge") }
    }
}

enum class OsName { WINDOWS, MAC, LINUX, UNKNOWN }
enum class OsArch { X86_32, X86_64, ARM64, UNKNOWN }
data class OsType(val name: OsName, val arch: OsArch)

val currentOsType = run {
    val gradleOs = OperatingSystem.current()
    val osName = when {
        gradleOs.isMacOsX -> OsName.MAC
        gradleOs.isWindows -> OsName.WINDOWS
        gradleOs.isLinux -> OsName.LINUX
        else -> OsName.UNKNOWN
    }

    val osArch = when (providers.systemProperty("sun.arch.data.model").get()) {
        "32" -> OsArch.X86_32
        "64" -> when (providers.systemProperty("os.arch").get().lowercase()) {
            "aarch64" -> OsArch.ARM64
            else -> OsArch.X86_64
        }
        else -> OsArch.UNKNOWN
    }

    OsType(osName, osArch)
}


val jsShellVersion = "2024-05-07-09-13-07-mozilla-central"
val jsShellSuffix = when (currentOsType) {
    OsType(OsName.LINUX, OsArch.X86_32) -> "linux-i686"
    OsType(OsName.LINUX, OsArch.X86_64) -> "linux-x86_64"
    OsType(OsName.MAC, OsArch.X86_64),
    OsType(OsName.MAC, OsArch.ARM64) -> "mac"
    OsType(OsName.WINDOWS, OsArch.X86_32) -> "win32"
    OsType(OsName.WINDOWS, OsArch.X86_64) -> "win64"
    else -> error("unsupported os type $currentOsType")
}

val jsShell by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val wasmEdgeVersion = libs.versions.wasmedge
val wasmEdgeSuffix = when (currentOsType) {
    OsType(OsName.LINUX, OsArch.X86_64) -> "manylinux_2_28_x86_64@tar.gz"
    OsType(OsName.MAC, OsArch.X86_64) -> "darwin_x86_64@tar.gz"
    OsType(OsName.MAC, OsArch.ARM64) -> "darwin_arm64@tar.gz"
    OsType(OsName.WINDOWS, OsArch.X86_32),
    OsType(OsName.WINDOWS, OsArch.X86_64) -> "windows@zip"
    else -> error("unsupported os type $currentOsType")
}
val wasmEdgeInnerSuffix = when (currentOsType.name) {
    OsName.LINUX -> "Linux"
    OsName.MAC -> "Darwin"
    OsName.WINDOWS -> "Windows"
    else -> error("unsupported os type $currentOsType")
}

val wasmEdge by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":js:js.tests"))
    testApi(intellijCore())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    jsShell("org.mozilla:jsshell:$jsShellVersion:$jsShellSuffix@zip")

    implicitDependencies("org.mozilla:jsshell:$jsShellVersion:win64@zip")
    implicitDependencies("org.mozilla:jsshell:$jsShellVersion:linux-x86_64@zip")
    implicitDependencies("org.mozilla:jsshell:$jsShellVersion:mac@zip")

    wasmEdge("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:$wasmEdgeSuffix")

    implicitDependencies("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:windows@zip")
    implicitDependencies("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:manylinux_2_28_x86_64@tar.gz")
    implicitDependencies("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:darwin_arm64@tar.gz")
}

val generationRoot = projectDir.resolve("tests-gen")

optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

fun Test.setupWasmStdlib(target: String) {
    @Suppress("LocalVariableName")
    val Target = target.capitalize()
    dependsOn(":kotlin-stdlib:compileKotlinWasm$Target")
    systemProperty("kotlin.wasm-$target.stdlib.path", "libraries/stdlib/build/classes/kotlin/wasm$Target/main")
    dependsOn(":kotlin-test:compileKotlinWasm$Target")
    systemProperty("kotlin.wasm-$target.kotlin.test.path", "libraries/kotlin.test/build/classes/kotlin/wasm$Target/main")
}

fun Test.setupGradlePropertiesForwarding() {
    val rootLocalProperties = Properties().apply {
        rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use {
            load(it)
        }
    }

    val allProperties = properties + rootLocalProperties

    val prefixForPropertiesToForward = "fd."
    for ((key, value) in allProperties) {
        if (key is String && key.startsWith(prefixForPropertiesToForward)) {
            systemProperty(key.substring(prefixForPropertiesToForward.length), value!!)
        }
    }
}

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")
val typescriptTestsDir = testDataDir.resolve("typescript-export")
val wasmTestDir = typescriptTestsDir.resolve("wasm")

fun generateTypeScriptTestFor(dir: String): TaskProvider<NpmTask> = tasks.register<NpmTask>("generate-ts-for-$dir") {
    val baseDir = wasmTestDir.resolve(dir)
    val mainTsFile = fileTree(baseDir).files.find { it.name.endsWith("__main.ts") } ?: return@register
    val mainJsFile = baseDir.resolve("${mainTsFile.nameWithoutExtension}.js")

    workingDir.set(testDataDir)

    inputs.file(mainTsFile)
    outputs.file(mainJsFile)
    outputs.upToDateWhen { mainJsFile.exists() }

    args.set(listOf("run", "generateTypeScriptTests", "--", "./typescript-export/wasm/$dir/tsconfig.json"))
}

val installTsDependencies by task<NpmTask> {
    val packageLockFile = testDataDir.resolve("package-lock.json")
    val nodeModules = testDataDir.resolve("node_modules")
    inputs.file(testDataDir.resolve("package.json"))
    outputs.file(packageLockFile)
    outputs.upToDateWhen { nodeModules.exists() }

    workingDir.set(testDataDir)
    args.set(listOf("install"))
}

val generateTypeScriptTests by parallel(
    beforeAll = installTsDependencies,
    tasksToRun = wasmTestDir
        .listFiles { it: File -> it.isDirectory }
        .map { generateTypeScriptTestFor(it.name) }
)

val unzipJsShell by task<Copy> {
    dependsOn(jsShell)
    from {
        zipTree(jsShell.singleFile)
    }
    into(layout.buildDirectory.dir("tools/jsshell-$jsShellSuffix-$jsShellVersion"))
}

val unzipWasmEdge by task<Copy> {
    dependsOn(wasmEdge)

    from {
        if (wasmEdge.singleFile.extension == "zip") {
            zipTree(wasmEdge.singleFile)
        } else {
            tarTree(wasmEdge.singleFile)
        }
    }

    val distDir = layout.buildDirectory.dir("tools")
    val currentOsTypeForConfigurationCache = currentOsType.name
    val resultDir = "WasmEdge-${wasmEdgeVersion.get()}-$wasmEdgeInnerSuffix"

    into(distDir)

    doLast {
        if (currentOsTypeForConfigurationCache !in setOf(OsName.MAC, OsName.LINUX)) return@doLast

        val wasmEdgeDirectory = distDir.get().dir(resultDir).asFile

        val libDirectory = wasmEdgeDirectory.toPath()
            .resolve(if (currentOsTypeForConfigurationCache == OsName.MAC) "lib" else "lib64")

        val targets = if (currentOsTypeForConfigurationCache == OsName.MAC)
            listOf("libwasmedge.0.1.0.dylib", "libwasmedge.0.1.0.tbd")
        else listOf("libwasmedge.so.0.1.0")

        targets.forEach {
            val target = libDirectory.resolve(it)
            val firstLink = libDirectory.resolve(it.replace("0.1.0", "0")).also(Files::deleteIfExists)
            val secondLink = libDirectory.resolve(it.replace(".0.1.0", "")).also(Files::deleteIfExists)

            Files.createSymbolicLink(firstLink, target)
            Files.createSymbolicLink(secondLink, target)
        }
    }
}

fun Test.setupSpiderMonkey() {
    dependsOn(unzipJsShell)
    val jsShellExecutablePath = File(unzipJsShell.get().destinationDir, "js").absolutePath
    systemProperty("javascript.engine.path.SpiderMonkey", jsShellExecutablePath)
}

fun Test.setupWasmEdge() {
    val wasmEdgeDirectory = unzipWasmEdge
        .map { it.destinationDir.resolve("WasmEdge-${wasmEdgeVersion.get()}-$wasmEdgeInnerSuffix") }

    inputs.dir(wasmEdgeDirectory)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("wasmEdgeDirectory")

    jvmArgumentProviders.add { listOf("-Dwasm.engine.path.WasmEdge=${wasmEdgeDirectory.get().resolve("bin/wasmedge")}") }
}

testsJar {}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateWasmTestsKt") {
    dependsOn(":compiler:generateTestData")
}

fun Project.wasmProjectTest(
    taskName: String,
    body: Test.() -> Unit = {}
): TaskProvider<Test> {
    return projectTest(
        taskName = taskName,
        parallel = true,
        jUnitMode = JUnitMode.JUnit5
    ) {
        workingDir = rootDir
        with(d8KotlinBuild) {
            setupV8()
        }
        with(nodeJsKotlinBuild) {
            setupNodeJs()
        }
        with(binaryenKotlinBuild) {
            setupBinaryen()
        }
        setupSpiderMonkey()
        setupWasmEdge()
        useJUnitPlatform()
        setupWasmStdlib("js")
        setupWasmStdlib("wasi")
        setupGradlePropertiesForwarding()
        systemProperty("kotlin.wasm.test.root.out.dir", "${layout.buildDirectory.get().asFile}/")
        body()
    }
}

// Test everything
wasmProjectTest("test")

wasmProjectTest("testFir") {
    dependsOn(generateTypeScriptTests)
    include("**/Fir*.class")
}

wasmProjectTest("testK1") {
    dependsOn(generateTypeScriptTests)
    include("**/K1*.class")
}

wasmProjectTest("diagnosticTest") {
    include("**/Diagnostics*.class")
}
