import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.internal.os.OperatingSystem
import java.net.URI

plugins {
    kotlin("jvm")
    alias(libs.plugins.gradle.node)
    id("java-test-fixtures")
    id("d8-configuration")
    id("nodejs-configuration")
    id("binaryen-configuration")
    id("project-tests-convention")
    id("test-inputs-check")
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
    if (cacheRedirectorEnabled) {
        distBaseUrl.set("https://cache-redirector.jetbrains.com/nodejs.org/dist")
    }
}

configureWasmEngineRepositories()
project.setupJscTasks()
project.setupSpiderMonkeyTasks()

dependencies {
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testFixturesApi(testFixtures(project(":wasm:wasm.tests")))
}

sourceSets {
    "main" { }
    "testFixtures" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

data class CustomCompilerVersion(val rawVersion: String) {
    val sanitizedVersion = rawVersion.replace('.', '_').replace('-', '_')
    override fun toString() = sanitizedVersion
}

fun Project.customCompilerTest(
    version: CustomCompilerVersion,
    taskName: String,
    tag: String,
    body: Test.() -> Unit = {},
): TaskProvider<out Task> {
    val customCompiler: Configuration = getOrCreateConfiguration("customCompiler_$version") {
        project.dependencies.add(name, "org.jetbrains.kotlin:kotlin-compiler-embeddable:${version.rawVersion}")
    }

    val runtimeDependencies: Configuration = getOrCreateConfiguration("customCompilerRuntimeDependencies_$version") {
        project.dependencies.add(name,"org.jetbrains.kotlin:kotlin-stdlib-wasm-js:${version.rawVersion}")
        project.dependencies.add(name, "org.jetbrains.kotlin:kotlin-test-wasm-js:${version.rawVersion}")
    }

    return projectTests.jsTestTask(taskName, tag) {
        addClasspathProperty(customCompiler, "kotlin.internal.wasm.test.compat.customCompilerClasspath")
        addClasspathProperty(runtimeDependencies, "kotlin.internal.wasm.test.compat.runtimeDependencies")
        systemProperty("kotlin.internal.wasm.test.compat.customCompilerVersion", version.rawVersion)
        systemProperty("kotlin.wasm.stdlib.klib.path", "libraries/stdlib/build/classes/kotlin/wasmJs/main")
        jvmArgumentProviders += objects.newInstance<AbsolutePathArgumentProvider>().apply {
            property.set("kotlin.wasm.test.root.out.dir")
            buildDirectory.set(layout.buildDirectory)
        }
        jvmArgumentProviders += objects.newInstance<AbsolutePathArgumentProvider>().apply {
            property.set("kotlin.wasm.test.node.dir")
            buildDirectory.set(node.nodeProjectDir)
        }
        setupJsc()
        setupSpiderMonkey()
        with(binaryenKotlinBuild) {
            setupBinaryen()
        }
        body()
    }
}

fun Project.customFirstStageTest(
    rawVersion: String,
): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)

    return customCompilerTest(
        version = version,
        taskName = "testCustomFirstStage_$version",
        tag = "custom-first-stage"
    )
}

fun Project.customSecondStageTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)
    return customCompilerTest(
        version = version,
        taskName = "testCustomSecondStage_$version",
        tag = "custom-second-stage"
    )
}

fun Project.customStagesAggregateTest(rawVersion: String): TaskProvider<out Task> {
    val version = CustomCompilerVersion(rawVersion)
    return customCompilerTest(
        version = version,
        taskName = "testMinimalInAggregate",
        tag = "aggregate",
    )
}

/* Custom-first-stage test tasks for different compiler versions. */
customFirstStageTest("2.3.0")
// TODO: Add a new task for the "custom-first-stage" test here.

/* Custom-second-stage test task for the two compiler major versions: previous one and the latest one . */
// TODO: Keep updating the following compiler versions to be the previous one and latest one(as as soon it's released).
customSecondStageTest("2.3.0")

// TODO: Keep updating the following compiler version to be the previous major one.
customStagesAggregateTest("2.3.0")

tasks.test {
    // The default test task does not resolve the necessary dependencies and does not set up the environment.
    // Making it disabled to avoid running it accidentally.
    enabled = false
}

projectTests {
    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateWasmJsKlibCompatibilityTestsKt", generateTestsInBuildDirectory = true)
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/klib/klib-compatibility/sanity")

    withWasmRuntime()
    withStdlibCommon()
}


// TODO KT-84080: unify the following duplicated code with `wasm/wasm.tests/build.gradle.kts`,
//      probably by moving it to `repo/gradle-build-conventions/project-tests-convention/src/main/kotlin/wasmTest.kt`
fun Project.configureWasmEngineRepositories() {
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
}

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

    private fun getJscRunnerContent(jscBinariesDir: File, osTypeName: OsName) = when (osTypeName) {
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

val Project.jslibs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.setupJscTasks() {
    val jscOsDependentRevision = when (currentOsType.name) {
        OsName.MAC -> jslibs.findVersion("jscSequoia")
        OsName.LINUX -> jslibs.findVersion("jscLinux")
        OsName.WINDOWS -> jslibs.findVersion("jscWindows")
        else -> error("unsupported os type $currentOsType")
    }.get().requiredVersion

    val jscOsDependentClassifier = when (currentOsType.name) {
        OsName.MAC -> "sequoia"
        OsName.LINUX -> "linux64"
        OsName.WINDOWS -> "win64"
        else -> error("unsupported os type $currentOsType")
    }

    val jsc by configurations.creating {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    dependencies {
        add("jsc", "org.jsc:jsc:$jscOsDependentRevision:$jscOsDependentClassifier")

        add("implicitDependencies", "org.jsc:jsc:${jslibs.findVersion("jscSequoia").get().requiredVersion}:sequoia")
        add("implicitDependencies", "org.jsc:jsc:${jslibs.findVersion("jscLinux").get().requiredVersion}:linux64")
        add("implicitDependencies", "org.jsc:jsc:${jslibs.findVersion("jscWindows").get().requiredVersion}:win64")
    }

    val toolsDirectory = layout.buildDirectory.dir("tools")
    val jscDirectory = toolsDirectory.map { it.dir("JavaScriptCore").asFile }
    val unzipJsc by tasks.registering(org.gradle.api.tasks.Copy::class) {
        dependsOn(jsc)
        from({
            zipTree(jsc.singleFile)
        })

        into(jscDirectory.map { it.resolve("jsc-$jscOsDependentClassifier-$jscOsDependentRevision") })
    }

    tasks.register<CreateJscRunner>("createJscRunner") {
        osTypeName.set(currentOsType.name)

        val runnerFileName = if (currentOsType.name == OsName.WINDOWS) "runJsc.cmd" else "runJsc"
        val runnerFilePath = jscDirectory.map { it.resolve(runnerFileName) }
        outputFile.fileProvider(runnerFilePath)

        inputDirectory.set(project.layout.dir(unzipJsc.map { it.destinationDir }))
    }
}

fun Project.setupSpiderMonkeyTasks() {
    val jsShellVersion = jslibs.findVersion("jsShell").get()
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

    dependencies {
        add("jsShell", "org.mozilla:jsshell:${jsShellVersion.requiredVersion}:$jsShellSuffix@zip")

        add("implicitDependencies", "org.mozilla:jsshell:${jsShellVersion.requiredVersion}:win64@zip")
        add("implicitDependencies", "org.mozilla:jsshell:${jsShellVersion.requiredVersion}:linux-x86_64@zip")
        add("implicitDependencies", "org.mozilla:jsshell:${jsShellVersion.requiredVersion}:mac@zip")
    }

    val toolsDirectory = layout.buildDirectory.dir("tools")
    val jsShellDirectory = toolsDirectory.map { it.dir("JsShell").asFile }
    val jsShellUnpackedDirectory = jsShellDirectory.map { it.resolve("jsshell-$jsShellSuffix-${jsShellVersion.requiredVersion}") }

    tasks.register<org.gradle.api.tasks.Copy>("unzipJsShell") {
        dependsOn(jsShell)
        from({
            zipTree(jsShell.singleFile)
        })
        into(jsShellUnpackedDirectory)
    }
}

fun Test.setupJsc() {
    val createJscRunner = project.tasks.named<CreateJscRunner>("createJscRunner")
    val jscRunnerExecutablePath = createJscRunner.flatMap { it.outputFile }

    jvmArgumentProviders += project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jscRunnerExecutablePath)
        property.set("javascript.engine.path.JavaScriptCore")
    }
}

fun Test.setupSpiderMonkey() {
    val unzipJsShell = project.tasks.named<Copy>("unzipJsShell")
    val jsShellExecutablePath = unzipJsShell
        .map { it.destinationDir }
        .map { it.resolve("js") }

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(jsShellExecutablePath)
        property.set("javascript.engine.path.SpiderMonkey")
    }
}

enum class OsName { WINDOWS, MAC, LINUX, UNKNOWN }
enum class OsArch { X86_32, X86_64, ARM64, UNKNOWN }
data class OsType(val name: OsName, val arch: OsArch)

val Project.currentOsType: OsType
    get() = run {
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
