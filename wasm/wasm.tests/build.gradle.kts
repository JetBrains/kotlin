import com.github.gradle.node.npm.task.NpmTask
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.smokeTestConfig
import java.util.*

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
    id("binaryen-configuration")
    id("nodejs-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
    id("wasmtime-configuration")
}

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
    distBaseUrl.set(null as String?)
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

val jsShell = configurations.create("jsShell") {
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

val wasmEdge = configurations.create("wasmEdge") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val jscOsDependentVersion = when (currentOsType.name) {
    OsName.MAC -> libs.versions.jscTahoe
    OsName.LINUX -> libs.versions.jscLinux
    OsName.WINDOWS -> libs.versions.jscWindows
    else -> error("unsupported os type $currentOsType")
}.get()

//https://youtrack.jetbrains.com/articles/KT-A-950/JavaScript-Core-Update-instruction
val jscOsDependentClassifier = when (currentOsType.name) {
    OsName.MAC -> "tahoe"
    OsName.LINUX -> "linux64"
    OsName.WINDOWS -> "win64"
    else -> error("unsupported os type $currentOsType")
}

val jscOsDependentRevision = when (currentOsType.name) {
    OsName.MAC -> libs.versions.jscTahoe
    OsName.LINUX -> libs.versions.jscLinux
    OsName.WINDOWS -> libs.versions.jscWindows
    else -> error("unsupported os type $currentOsType")
}.get()

val jsc = configurations.create("jsc") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val wasmtimeVersion = libs.versions.wasmtime

configurations.create("wasmtime") {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesImplementation(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesImplementation(intellijCore())
    testFixturesImplementation(project(":wasm:wasm.frontend"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testImplementation(project(":wasm:wasm.frontend"))
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

    implicitDependencies("org.jsc:jsc:${libs.versions.jscTahoe.get()}:tahoe")
    implicitDependencies("org.jsc:jsc:${libs.versions.jscLinux.get()}:linux64")
    implicitDependencies("org.jsc:jsc:${libs.versions.jscWindows.get()}:win64")

    implicitDependencies("bytecodealliance.wasmtime:wasmtime:${wasmtimeVersion.get()}:x86_64-windows@zip")
    implicitDependencies("bytecodealliance.wasmtime:wasmtime:${wasmtimeVersion.get()}:x86_64-linux@tar.xz")
    implicitDependencies("bytecodealliance.wasmtime:wasmtime:${wasmtimeVersion.get()}:aarch64-macos@tar.xz")
}

optInToExperimentalCompilerApi()

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")

val testJsFile = testDataDir.resolve("test.js")
val packageJsonFile = testDataDir.resolve("package.json")
val packageLockJsonFile = testDataDir.resolve("package-lock.json")

val prepareNpmTestData = tasks.register<Copy>("prepareNpmTestData") {
    from(testJsFile)
    from(packageJsonFile)
    from(packageLockJsonFile)
    into(node.nodeProjectDir)
}

val npmInstall = tasks.named("npmInstall", NpmTask::class) {
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

optInToK1Deprecation()
fun Test.setupGradlePropertiesForwarding() {
    val rootLocalProperties = Properties().apply {
        rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use {
            load(it)
        }
    }

    val prefixForPropertiesToForward = "fd."
    val filteredProperties: Provider<Map<String, String>> = providers.gradlePropertiesPrefixedBy(prefixForPropertiesToForward)
    val allProperties = filteredProperties.get() + rootLocalProperties

    for ((key, value) in allProperties) {
        if (key is String && key.startsWith(prefixForPropertiesToForward)) {
            systemProperty(key.substring(prefixForPropertiesToForward.length), value!!)
        }
    }
}

val toolsDirectory = layout.buildDirectory.dir("tools")

val jsShellDirectory = toolsDirectory.map { it.dir("JsShell").asFile }
val jsShellUnpackedDirectory = jsShellDirectory.map { it.resolve("jsshell-$jsShellSuffix-${jsShellVersion.get()}") }
val unzipJsShell = tasks.register<Copy>("unzipJsShell") {
    dependsOn(jsShell)
    from {
        zipTree(jsShell.singleFile)
    }
    into(jsShellUnpackedDirectory)
}

val unzipWasmEdge = tasks.register<UnzipWasmEdge>("unzipWasmEdge") {
    from.setFrom(wasmEdge)

    val currentOsTypeForConfigurationCache = currentOsType.name

    into.fileProvider(toolsDirectory.map { it.dir("WasmEdge").asFile })

    getIsWindows.set(currentOsTypeForConfigurationCache !in setOf(OsName.MAC, OsName.LINUX))
    getIsMac.set(currentOsTypeForConfigurationCache == OsName.MAC)
}

val jscDirectory = toolsDirectory.map { it.dir("JavaScriptCore").asFile }
val unzipJsc = tasks.register<UnzipJsc>("unzipJsc") {
    from.setFrom(jsc)

    into.fileProvider(jscDirectory.map { it.resolve("jsc-$jscOsDependentClassifier-$jscOsDependentRevision") })

    val isLinux = currentOsType.name == OsName.LINUX
    getIsLinux.set(isLinux)
}

val createJscRunner = tasks.register<CreateJscRunner>("createJscRunner") {
    osTypeName.set(currentOsType.name)

    val runnerFileName = if (currentOsType.name == OsName.WINDOWS) "runJsc.cmd" else "runJsc"
    val runnerFilePath = jscDirectory.map { it.resolve(runnerFileName) }
    outputFile.fileProvider(runnerFilePath)

    inputDirectory.set(unzipJsc.flatMap { it.into })
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

    systemProperty(
        "javascript.engine.JavaScriptCore.EnableOnWindows",
        kotlinBuildProperties.booleanProperty("kotlin.enable.tests.jsc.on.windows").get()
    )
}

testsJar {}

projectTests {
    testGenerator(
        "org.jetbrains.kotlin.generators.tests.GenerateWasmTestsKt",
        generateTestsInBuildDirectory = true,
    )

    fun wasmProjectTest(
        taskName: String,
        tags: String? = null,
        skipInLocalBuild: Boolean = true,
        body: Test.() -> Unit = {},
    ) {
        testTask(
            taskName = taskName,
            skipInLocalBuild = skipInLocalBuild,
            enableGroupingTestEngine = true,
            maxHeapSize = testMaxHeapSizeLarge,
        ) {
            with(d8KotlinBuild) {
                setupV8()
            }
            with(wasmtimeKotlinBuild) {
                setupWasmtime()
            }
            with(wasmNodeJsKotlinBuild) {
                setupNodeJs(nodejsVersion)
                dependsOn(":js:js.tests:npmInstall")
            }
            // it is necessary for TypeScript tests
            with(nodeJsKotlinBuild) {
                setupNodeJs(nodejsVersion)
                dependsOn(":js:js.tests:npmInstall")
            }
            with(binaryenKotlinBuild) {
                setupBinaryen()
            }
            setupSpiderMonkey()
            setupWasmEdge()
            setupJsc()
            // Note: arbitrary JUnit tag expressions can be used here.
            useJUnitPlatform {
                tags?.let { includeTags(it) }
            }
            setupGradlePropertiesForwarding()

            // Most of the batches of a Wasm test run consist of a single isolated test, and the grouping stage of
            // such a batch is single-threaded, so processing only a couple of batches at a time leaves most of the
            // worker threads idle. Measured on `WasmJsCodegenBoxTestGenerated` (8393 tests): 6m32s with the
            // conservative engine default of 2 vs 6m01s with 6, at an unchanged limit on the tests in flight.
            // Higher values do not help any more: 7 is within noise and 10 is slower.
            systemProperty("kotlin.test.grouping.engine.simultaneous.batches", "6")

            addAbsoluteDirectoryProperty(layout.buildDirectory, "kotlin.wasm.test.root.out.dir")
            addAbsoluteDirectoryProperty(node.nodeProjectDir, "kotlin.wasm.test.node.dir")
            body()
            dependsOn(npmInstall)
        }
    }

    // The tags are declared in testFixtures/org/jetbrains/kotlin/wasm/test/WasmTestGroups.kt
    val icTag = "wasmIc"
    val jsBoxTag = "wasmJsBox"
    val jsSplittingTag = "wasmJsSplitting"
    val jsMultiModuleTag = "wasmJsMultiModule"
    val wasiBoxTag = "wasmWasiBox"
    val extraTag = "wasmFirCompilerExtra"

    val allTags = listOf(
        icTag, jsBoxTag, jsSplittingTag,
        jsMultiModuleTag, wasiBoxTag, extraTag
    )

    // Test everything, intended to use locally
    wasmProjectTest("test", skipInLocalBuild = false) {
        smokeTestConfig = SmokeTestConfig.Enabled(autoSmokeTestPercentage = 1)
    }

    // The nine tasks below split the content of the `test` task into disjoint groups.
    // Tests without any of the group tags are run by the `wasmMiscTest` task.
    // The `wasmFirCompilerExtraTest` task is excluded from aggregate `wasmFirCompilerTest` task.
    wasmProjectTest("wasmFirCompilerExtraTest", tags = extraTag)
    wasmProjectTest("wasmJsBoxTest", tags = jsBoxTag)
    wasmProjectTest("wasmJsSplittingTest", tags = jsSplittingTag)
    wasmProjectTest("wasmJsMultiModuleTest", tags = jsMultiModuleTag)
    wasmProjectTest("wasmWasiBoxTest", tags = wasiBoxTag)
    wasmProjectTest("wasmIcTest", tags = "$icTag & !$extraTag")
    wasmProjectTest("wasmMiscTest", tags = allTags.joinToString(" & ") { "!$it" })

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/debug")
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
    from(project(":compiler").isolated.projectDirectory.dir("testData/debug")) {
        into("debugTestHelpers")
        include("wasmTestHelpers/")
    }
}
