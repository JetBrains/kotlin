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
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
}

repositories {
    ivy {
        url = URI("https://archive.mozilla.org/pub/firefox/releases/")
        patternLayout {
            artifact("[revision]/jsshell/[artifact]-[classifier].[ext]")
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


val jsShellVersion = "134.0.2"
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
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(intellijCore())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
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

optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
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
    inputs.file(packageLockFile)
    outputs.upToDateWhen { nodeModules.exists() }

    workingDir.set(testDataDir)
    npmCommand.set(listOf("ci"))
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
    jvmArgumentProviders += this.project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jsShellExecutablePath)
        property.set("javascript.engine.path.SpiderMonkey")
    }
}

fun Test.setupWasmEdge() {
    val wasmEdgeDirectory: Provider<File> = unzipWasmEdge
        .map { it.destinationDir.resolve("WasmEdge-${wasmEdgeVersion.get()}-$wasmEdgeInnerSuffix") }

    jvmArgumentProviders += this.project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(wasmEdgeDirectory.map { it.resolve("bin/wasmedge") })
        property.set("wasm.engine.path.WasmEdge")
    }
}

testsJar {}

projectTests {
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/ir")
//    testData(project(":compiler").isolated, "testData/klib")

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateWasmTestsKt")

    fun wasmProjectTest(taskName: String, skipInLocalBuild: Boolean = false, body: Test.() -> Unit = {}) {
        testTask(
            taskName = taskName,
            jUnitMode = JUnitMode.JUnit5,
            skipInLocalBuild = skipInLocalBuild,
        ) {
            with(d8KotlinBuild) {
                setupV8()
            }
            with(wasmNodeJsKotlinBuild) {
                setupNodeJs(nodejsVersion)
            }
            with(binaryenKotlinBuild) {
                setupBinaryen()
            }
            setupSpiderMonkey()
            setupWasmEdge()
            useJUnitPlatform()
            setupGradlePropertiesForwarding()
            systemProperty("kotlin.wasm.test.root.out.dir", "${layout.buildDirectory.get().asFile}/")
            body()
        }
    }

    // Test everything
    wasmProjectTest("test")

    wasmProjectTest("testFir", skipInLocalBuild = true) {
        dependsOn(generateTypeScriptTests)
        include("**/Fir*.class")
    }

    wasmProjectTest("diagnosticTest", skipInLocalBuild = true) {
        include("**/Diagnostics*.class")
    }

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen/box")
    testData(project(":compiler").isolated, "testData/codegen/boxInline")
    testData(project(":compiler").isolated, "testData/codegen/boxWasmJsInterop")
    testData(project(":compiler").isolated, "testData/codegen/boxWasmWasi")
    testData(project(":compiler").isolated, "testData/debug/stepping")
    testData(project(":compiler").isolated, "testData/klib/partial-linkage")
    testData(project(":compiler").isolated, "testData/klib/resolve")
    testData(project(":compiler").isolated, "testData/klib/syntheticAccessors")
    testData(project(":compiler").isolated, "testData/klib/__utils__")

    testData(project(":js:js.translator").isolated, "testData/incremental")
    testData(project(":js:js.translator").isolated, "testData/box")
    testData(project(":js:js.translator").isolated, "testData/typescript-export/wasm/")

    withWasmRuntime()
}

tasks.processTestFixturesResources.configure {
    from(project.layout.projectDirectory.dir("_additionalFilesForTests"))
    from(project(":compiler").layout.projectDirectory.dir("testData/debug")) {
        into("debugTestHelpers")
        include("wasmTestHelpers/")
    }
}
