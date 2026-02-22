import com.github.gradle.node.npm.task.NpmTask
import org.gradle.internal.os.OperatingSystem
import java.net.URI
import java.util.*

plugins {
    kotlin("jvm")
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
        url = URI("https://packages.jetbrains.team/files/p/kt/kotlin-file-dependencies/javascriptcore/")
        patternLayout {
            artifact("[classifier]_[revision].zip")
        }
        metadataSources { artifact() }
        content { includeModule("org.jsc", "jsc") }
    }
    githubRelease("WasmEdge", "WasmEdge", groupAlias = "org.wasmedge", revisionPrefix = "")
    githubRelease("bytecodealliance", "wasmtime", groupAlias = "dev.wasmtime")
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

val wasmtimeVersion = libs.versions.wasmtime
val wasmtimePlatformSuffix = when (currentOsType) {
    OsType(OsName.LINUX, OsArch.X86_64) -> "x86_64-linux"
    OsType(OsName.MAC, OsArch.X86_64) -> "x86_64-macos"
    OsType(OsName.MAC, OsArch.ARM64) -> "aarch64-macos"
    OsType(OsName.WINDOWS, OsArch.X86_32),
    OsType(OsName.WINDOWS, OsArch.X86_64) -> "x86_64-windows"
    else -> error("unsupported os type $currentOsType")
}
val wasmtimeSuffix = wasmtimePlatformSuffix + "@" + when (currentOsType.name) {
    OsName.LINUX -> "tar.xz"
    OsName.MAC -> "tar.xz"
    OsName.WINDOWS -> "zip"
    else -> error("unsupported os type $currentOsType")
}

val wasmtime by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesImplementation(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesImplementation(intellijCore())
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

    wasmEdge("org.wasmedge:WasmEdge:${wasmEdgeVersion.get()}:$wasmEdgeSuffix")

    implicitDependencies("org.wasmedge:WasmEdge:${wasmEdgeVersion.get()}:windows@zip")
    implicitDependencies("org.wasmedge:WasmEdge:${wasmEdgeVersion.get()}:manylinux_2_28_x86_64@tar.gz")
    implicitDependencies("org.wasmedge:WasmEdge:${wasmEdgeVersion.get()}:darwin_arm64@tar.gz")

    jsc("org.jsc:jsc:$jscOsDependentRevision:$jscOsDependentClassifier")

    implicitDependencies("org.jsc:jsc:${libs.versions.jscSequoia.get()}:sequoia")
    implicitDependencies("org.jsc:jsc:${libs.versions.jscLinux.get()}:linux64")
    implicitDependencies("org.jsc:jsc:${libs.versions.jscWindows.get()}:win64")

    wasmtime("dev.wasmtime:wasmtime:${wasmtimeVersion.get()}:$wasmtimeSuffix")

    implicitDependencies("dev.wasmtime:wasmtime:${wasmtimeVersion.get()}:x86_64-windows@zip")
    implicitDependencies("dev.wasmtime:wasmtime:${wasmtimeVersion.get()}:x86_64-linux@tar.xz")
    implicitDependencies("dev.wasmtime:wasmtime:${wasmtimeVersion.get()}:aarch64-macos@tar.xz")
}

optInToExperimentalCompilerApi()

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")

val testJsFile = testDataDir.resolve("test.js")
val packageJsonFile = testDataDir.resolve("package.json")
val packageLockJsonFile = testDataDir.resolve("package-lock.json")

val prepareNpmTestData by task<Copy> {
    from(testJsFile)
    from(packageJsonFile)
    from(packageLockJsonFile)
    into(node.nodeProjectDir)
}

val npmInstall by tasks.getting(NpmTask::class) {
    val packageLockFile = testDataDir.resolve("package-lock.json")

    inputs.file(node.nodeProjectDir.file("package.json"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("packageJson")

    inputs.file(packageLockFile)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("packageLockFile")
    outputs.upToDateWhen { packageLockFile.exists() }

    workingDir.fileProvider(node.nodeProjectDir.asFile)
    dependsOn(prepareNpmTestData)
    npmCommand.set(listOf("ci"))
}

sourceSets {
    "main" { }
    "test" {
        projectDefault()
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

val toolsDirectory = layout.buildDirectory.dir("tools")

val jsShellDirectory = toolsDirectory.map { it.dir("JsShell").asFile }
val jsShellUnpackedDirectory = jsShellDirectory.map { it.resolve("jsshell-$jsShellSuffix-${jsShellVersion.get()}") }
val unzipJsShell by task<Copy> {
    dependsOn(jsShell)
    from {
        zipTree(jsShell.singleFile)
    }
    into(jsShellUnpackedDirectory)
}

val unzipWasmEdge by task<UnzipWasmEdge> {
    from.setFrom(wasmEdge)

    val currentOsTypeForConfigurationCache = currentOsType.name

    into.fileProvider(toolsDirectory.map { it.dir("WasmEdge").asFile })

    getIsWindows.set(currentOsTypeForConfigurationCache !in setOf(OsName.MAC, OsName.LINUX))
    getIsMac.set(currentOsTypeForConfigurationCache == OsName.MAC)
}


val jscDirectory = toolsDirectory.map { it.dir("JavaScriptCore").asFile }
val unzipJsc by task<UnzipJsc> {
    from.setFrom(jsc)

    into.fileProvider(jscDirectory.map { it.resolve("jsc-$jscOsDependentClassifier-$jscOsDependentRevision") })

    val isLinux = currentOsType.name == OsName.LINUX
    getIsLinux.set(isLinux)
}

val createJscRunner by task<CreateJscRunner> {
    osTypeName.set(currentOsType.name)

    val runnerFileName = if (currentOsType.name == OsName.WINDOWS) "runJsc.cmd" else "runJsc"
    val runnerFilePath = jscDirectory.map { it.resolve(runnerFileName) }
    outputFile.fileProvider(runnerFilePath)

    inputDirectory.set(unzipJsc.flatMap { it.into })
}

val unzipWasmtime by task<UnzipWasmtime> {
    from.setFrom(wasmtime)

    val currentOsTypeForConfigurationCache = currentOsType.name

    getIsWindows.set(currentOsTypeForConfigurationCache !in setOf(OsName.MAC, OsName.LINUX))

    val wasmtimeDirectoryName: Provider<String> = wasmtimeVersion.map { version -> "wasmtime-$version-$wasmtimePlatformSuffix" }

    into.set(
        toolsDirectory.zip(wasmtimeDirectoryName) { toolsDir: Directory, wasmtimeDir: String ->
            toolsDir.dir("Wasmtime").dir(wasmtimeDir)
        }
    )
}

fun Test.setupSpiderMonkey() {
    val jsShellExecutablePath = unzipJsShell
        .map { it.destinationDir }
        .map { it.resolve("js") }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jsShellExecutablePath)
        property.set("javascript.engine.path.SpiderMonkey")
    }
}

fun Test.setupWasmEdge() {
    val wasmEdgeExecutablePath = unzipWasmEdge
        .flatMap { task ->
            task.into.file("bin/wasmedge")
        }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(wasmEdgeExecutablePath)
        property.set("wasm.engine.path.WasmEdge")
    }
}

fun Test.setupJsc() {
    val jscRunnerExecutablePath = createJscRunner
        .flatMap { it.outputFile }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jscRunnerExecutablePath)
        property.set("javascript.engine.path.JavaScriptCore")
    }
}

fun Test.setupWasmtime() {
    val wasmtime = unzipWasmtime
        .flatMap { it.into.dir("wasmtime-v${wasmtimeVersion.get()}-$wasmtimePlatformSuffix") }
        .map { it.file("wasmtime") }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(wasmtime)
        property.set("wasm.engine.path.Wasmtime")
    }
}

testsJar {}

projectTests {
    testGenerator(
        "org.jetbrains.kotlin.generators.tests.GenerateWasmTestsKt",
        generateTestsInBuildDirectory = true,
    )

    fun wasmProjectTest(taskName: String, skipInLocalBuild: Boolean = false, body: Test.() -> Unit = {}) {
        testTask(
            taskName = taskName,
            jUnitMode = JUnitMode.JUnit5,
            skipInLocalBuild = skipInLocalBuild,
            maxHeapSizeMb = 6144
        ) {
            with(d8KotlinBuild) {
                setupV8()
            }
            with(wasmNodeJsKotlinBuild) {
                setupNodeJs(nodejsVersion)
                dependsOn(":js:js.tests:npmInstall")
            }
            with(binaryenKotlinBuild) {
                setupBinaryen()
            }
            setupSpiderMonkey()
            setupWasmEdge()
            setupJsc()
            setupWasmtime()
            useJUnitPlatform()
            setupGradlePropertiesForwarding()
            jvmArgumentProviders += objects.newInstance<AbsolutePathArgumentProvider>().apply {
                property.set("kotlin.wasm.test.root.out.dir")
                buildDirectory.set(layout.buildDirectory)
            }
            jvmArgumentProviders += objects.newInstance<AbsolutePathArgumentProvider>().apply {
                property.set("kotlin.wasm.test.node.dir")
                buildDirectory.set(node.nodeProjectDir)
            }
            body()
            dependsOn(npmInstall)
        }
    }

    // Test everything
    wasmProjectTest("test") {
        include("**/*.class")
        exclude("**/*SingleModule*TestGenerated.class")
        exclude("**/*MultiModule*TestGenerated.class")
    }

    wasmProjectTest("diagnosticTest", skipInLocalBuild = true) {
        include("**/Diagnostics*.class")
    }

    wasmProjectTest("wasmFirCompilerExtraTest") {
        include("**/*SingleModule*TestGenerated.class")
        include("**/*MultiModule*TestGenerated.class")
    }

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/debug/stepping")
    testData(project(":compiler").isolated, "testData/ir/irText")
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
