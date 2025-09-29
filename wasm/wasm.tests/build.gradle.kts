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
    ivy {
        url = URI("https://packages.jetbrains.team/files/p/kt/kotlin-file-dependencies/javascriptcore/")
        patternLayout {
            artifact("[classifier]_[revision].zip")
        }
        metadataSources { artifact() }
        content { includeModule("org.jsc", "jsc") }
    }
}

enum class OsName { WINDOWS, MAC, LINUX, UNKNOWN }
enum class OsArch { X86_32, X86_64, ARM64, UNKNOWN }
data class OsType(val name: OsName, val arch: OsArch)


abstract class CreateJscRunner : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val osTypeName: Property<OsName>

    @TaskAction
    fun action() {
        val jscBinariesDir = inputDirectory.get().asFile.let { dir ->
            when (osTypeName.get()) {
                OsName.MAC -> dir.resolve("Release")
                OsName.LINUX -> dir
                OsName.WINDOWS -> dir.resolve("bin")
                else -> error("unsupported os name")
            }
        }

        val runnerContent = getJscRunnerContent(jscBinariesDir, osTypeName.get())
        val outputFile = outputFile.get().asFile
        with(outputFile) {
            writeText(runnerContent)
            setExecutable(true)
        }
    }

    fun getJscRunnerContent(jscBinariesDir: File, osTypeName: OsName) = when (osTypeName) {
        OsName.MAC ->
            """#!/usr/bin/env bash
DYLD_FRAMEWORK_PATH="$jscBinariesDir" DYLD_LIBRARY_PATH="$jscBinariesDir" "$jscBinariesDir/jsc" "$@"
"""
        OsName.LINUX ->
            """#!/usr/bin/env bash
LD_LIBRARY_PATH="$jscBinariesDir/lib" exec "$jscBinariesDir/lib/ld-linux-x86-64.so.2" "$jscBinariesDir/bin/jsc" "$@"
"""
        OsName.WINDOWS ->
            """@echo off
"$jscBinariesDir\\jsc.exe" %*
"""
        else -> error("unsupported os type $osTypeName")
    }
}

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

val jsShellVersion = libs.versions.jsShell
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

val jscOsDependentVersion = when (currentOsType.name) {
    OsName.MAC -> libs.versions.jscSequoia
    OsName.LINUX -> libs.versions.jscLinux
    OsName.WINDOWS -> libs.versions.jscWindows
    else -> error("unsupported os type $currentOsType")
}.get()

//https://youtrack.jetbrains.com/articles/KT-A-950/JavaScript-Core-Update-instruction
val jscOsDependentClassifier = when (currentOsType.name) {
    OsName.MAC -> "sequoia"
    OsName.LINUX -> "linux64"
    OsName.WINDOWS -> "win64"
    else -> error("unsupported os type $currentOsType")
}

val jscOsDependentRevision = when (currentOsType.name) {
    OsName.MAC -> libs.versions.jscSequoia
    OsName.LINUX -> libs.versions.jscLinux
    OsName.WINDOWS -> libs.versions.jscWindows
    else -> error("unsupported os type $currentOsType")
}.get()


val jsc by configurations.creating {
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

    implicitDependencies("org.nodejs:node:$nodejsVersion:win-x64@zip")
    implicitDependencies("org.nodejs:node:$nodejsVersion:linux-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsVersion:darwin-arm64@tar.gz")

    jsShell("org.mozilla:jsshell:${jsShellVersion.get()}:$jsShellSuffix@zip")

    implicitDependencies("org.mozilla:jsshell:${jsShellVersion.get()}:win64@zip")
    implicitDependencies("org.mozilla:jsshell:${jsShellVersion.get()}:linux-x86_64@zip")
    implicitDependencies("org.mozilla:jsshell:${jsShellVersion.get()}:mac@zip")

    wasmEdge("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:$wasmEdgeSuffix")

    implicitDependencies("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:windows@zip")
    implicitDependencies("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:manylinux_2_28_x86_64@tar.gz")
    implicitDependencies("org.wasmedge:wasmedge:${wasmEdgeVersion.get()}:darwin_arm64@tar.gz")

    jsc("org.jsc:jsc:$jscOsDependentRevision:$jscOsDependentClassifier")

    implicitDependencies("org.jsc:jsc:${libs.versions.jscSequoia.get()}:sequoia")
    implicitDependencies("org.jsc:jsc:${libs.versions.jscLinux.get()}:linux64")
    implicitDependencies("org.jsc:jsc:${libs.versions.jscWindows.get()}:win64")
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
val toolsDirectory = layout.buildDirectory.dir("tools")

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


val jsShellDirectory = toolsDirectory.map { it.dir("JsShell").asFile }
val jsShellUnpackedDirectory = jsShellDirectory.map { it.resolve("jsshell-$jsShellSuffix-${jsShellVersion.get()}") }
val unzipJsShell by task<Copy> {
    dependsOn(jsShell)
    from {
        zipTree(jsShell.singleFile)
    }
    into(jsShellUnpackedDirectory)
}

val wasmEdgeDirectory = toolsDirectory.map { it.dir("WasmEdge").asFile }
val wasmEdgeDirectoryName = wasmEdgeVersion.map { version -> "WasmEdge-$version-$wasmEdgeInnerSuffix" }
val wasmEdgeUnpackedDirectory = wasmEdgeDirectory.map { it.resolve(wasmEdgeDirectoryName.get()) }
val unzipWasmEdge by task<Copy> {
    dependsOn(wasmEdge)

    val wasmEdgeDirectory = wasmEdgeDirectory
    val currentOsTypeForConfigurationCache = currentOsType.name
    val wasmEdgeUnpackedDirectory = wasmEdgeUnpackedDirectory

    from {
        if (wasmEdge.singleFile.extension == "zip") {
            zipTree(wasmEdge.singleFile)
        } else {
            tarTree(wasmEdge.singleFile)
        }
    }
    into(wasmEdgeDirectory)
    inputs.property("currentOsTypeForConfigurationCache", currentOsTypeForConfigurationCache)

    doLast {
        if (currentOsTypeForConfigurationCache !in setOf(OsName.MAC, OsName.LINUX)) return@doLast

        val unpackedWasmEdgeDirectory = wasmEdgeUnpackedDirectory.get().toPath()

        val libDirectory = unpackedWasmEdgeDirectory
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


val jscDirectory = toolsDirectory.map { it.dir("JavaScriptCore").asFile }
val jscUnpackedDirectory = jscDirectory.map { it.resolve("jsc-$jscOsDependentClassifier-$jscOsDependentRevision") }
val unzipJsc by task<Copy> {
    dependsOn(jsc)
    from { zipTree(jsc.singleFile) }

    val jscUnpackedDirectory = jscUnpackedDirectory
    into(jscUnpackedDirectory)

    val isLinux = currentOsType.name == OsName.LINUX
    inputs.property("isLinux", isLinux)

    doLast {
        if (isLinux) {
            val libDirectory = File(jscUnpackedDirectory.get(), "lib")
            for (file in libDirectory.listFiles()) {
                if (file.isFile && file.length() < 100) { // seems unpacked file link
                    val linkTo = file.readText()
                    file.delete()
                    Files.createSymbolicLink(file.toPath(), File(linkTo).toPath())
                }
            }
        }
    }
}

val createJscRunner by task<CreateJscRunner> {
    osTypeName.set(currentOsType.name)

    val runnerFileName = if (currentOsType.name == OsName.WINDOWS) "runJsc.cmd" else "runJsc"
    val runnerFilePath = jscDirectory.map { it.resolve(runnerFileName) }
    outputFile.fileProvider(runnerFilePath)

    inputDirectory.fileProvider(unzipJsc.map { it.outputs.files.singleFile })
}

fun Test.setupSpiderMonkey() {
    val jsShellExecutablePath = unzipJsShell
        .map { it.outputs.files.singleFile }
        .map { it.resolve("js").absolutePath }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jsShellExecutablePath)
        property.set("javascript.engine.path.SpiderMonkey")
    }
}

fun Test.setupWasmEdge() {
    val wasmEdgeExecutablePath = unzipWasmEdge
        .map { it.outputs.files.singleFile }
        .map { it.resolve(wasmEdgeDirectoryName.get()) }
        .map { it.resolve("bin/wasmedge").absolutePath }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(wasmEdgeExecutablePath)
        property.set("wasm.engine.path.WasmEdge")
    }
}

fun Test.setupJsc() {
    val jscRunnerExecutablePath = createJscRunner
        .map { it.outputFile.asFile.get() }
        .map { it.absolutePath }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jscRunnerExecutablePath)
        property.set("javascript.engine.path.JavaScriptCore")
    }
}

testsJar {}

projectTests {
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
            setupJsc()
            useJUnitPlatform()
            setupGradlePropertiesForwarding()
            val buildDirectory = layout.buildDirectory.map { "${it.asFile}/" }
            jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
                classpath.from(buildDirectory)
                property.set("kotlin.wasm.test.root.out.dir")
            }
            body()
        }
    }

    // Test everything
    wasmProjectTest("test") {
        dependsOn(generateTypeScriptTests)
        include("**/*.class")
    }

    wasmProjectTest("diagnosticTest", skipInLocalBuild = true) {
        include("**/Diagnostics*.class")
    }

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/debug/stepping")
    testData(project(":compiler").isolated, "testData/ir")
    testData(project(":compiler").isolated, "testData/loadJava")
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
