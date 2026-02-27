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
        githubRelease("WasmEdge", "WasmEdge", groupAlias = "org.wasmedge", revisionPrefix = "")
        githubRelease("bytecodealliance", "wasmtime", groupAlias = "dev.wasmtime")
    }
}
