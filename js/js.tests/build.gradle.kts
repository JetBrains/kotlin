import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.testFederation.SmokeTestConfig
import org.jetbrains.kotlin.testFederation.TemporaryTestFederationApi
import org.jetbrains.kotlin.testFederation.smokeTestConfig
import java.util.*

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
    // TODO: uncomment this line after bootstrap
    // id("swc-configuration")
    id("nodejs-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

node {
    download.set(true)
    version.set(nodejsLtsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
    distBaseUrl.set(null as String?)
}

val testJsRuntime = configurations.create("testJsRuntime") {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

dependencies {
    testFixturesApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(protobufFull())
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesApi(testFixtures(project(":kotlin-util-klib")))
    testFixturesApi(testFixtures(project(":compiler:ir.backend.common")))

    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:cli-js"))
    testCompileOnly(project(":compiler:util"))
    testCompileOnly(intellijCore())
    testFixturesApi(project(":compiler:backend.js"))
    testFixturesApi(project(":js:js.parser"))
    testFixturesApi(project(":js:js.translator"))
    testFixturesApi(project(":js:typescript-export-standalone"))
    testFixturesApi(project(":compiler:incremental-compilation-impl"))
    testFixturesImplementation(project(":kotlin-util-klib-metadata"))
    testFixturesImplementation(project(":wasm:wasm.frontend"))
    testFixturesApi(testFixtures(project(":kotlin-build-common")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))

    testFixturesApi(intellijCore())
    testFixturesApi(project(":compiler:frontend"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":compiler:util"))

    testRuntimeOnly(libs.guava)
    testRuntimeOnly(intellijJDom())

    testRuntimeOnly(kotlinStdlib())
    testJsRuntime(kotlinStdlib())
    testJsRuntime(kotlinTest("js")) // to be sure that kotlin-test-js built before tests run
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(project(":compiler:ir.backend.common"))
    testRuntimeOnly(project(":kotlin-util-klib-abi"))
    testRuntimeOnly(commonDependency("org.fusesource.jansi", "jansi"))

    // these dependencies shouldn't be exposed to other modules
    // to avoid potential clashes in cases when another module
    // also needs one of these dependencies but of different
    // version (e.g. tests of kotlinx.serialization)
    testFixturesCompileOnly(libs.kotlinx.serialization.json)
    testRuntimeOnly(libs.kotlinx.serialization.json)

    implicitDependencies("org.nodejs:node:$nodejsLtsVersion:win-x64@zip")
    implicitDependencies("org.nodejs:node:$nodejsLtsVersion:linux-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsLtsVersion:darwin-x64@tar.gz")
    implicitDependencies("org.nodejs:node:$nodejsLtsVersion:darwin-arm64@tar.gz")
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()

sourceSets {
    "main" { }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")

fun Test.setUpJsBoxTests() {
    with(nodeJsKotlinBuild) {
        setupNodeJs(nodejsVersion)
    }

    dependsOn(npmInstall)

    systemProperty(
        "overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: "false"
    )

    forwardProperties()

    @OptIn(TemporaryTestFederationApi::class)
    smokeTestConfig = SmokeTestConfig.Enabled(autoSmokeTestPercentage = 1)
}

fun Test.forwardProperties() {
    val rootLocalProperties = Properties().apply {
        File(rootDir, "local.properties").takeIf { it.exists() }?.inputStream()?.use {
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

projectTests {
    jsTestTask {
        setUpJsBoxTests()
    }

    jsTestTask(taskName = "jsTest", tag = "!es6", skipInLocalBuild = true) {
        setUpJsBoxTests()
    }

    jsTestTask(taskName = "jsES6Test", tag = "es6", skipInLocalBuild = true) {
        setUpJsBoxTests()
    }

    testTask("invalidationTest", skipInLocalBuild = true) {
        useJsIrBoxTests(buildDir = layout.buildDirectory)
        include("org/jetbrains/kotlin/incremental/*")
        forwardProperties()
    }

    testData(project(":compiler").isolated, "testData/diagnostics")
    testData(project(":compiler").isolated, "testData/codegen")
    testData(project(":compiler").isolated, "testData/ir")
    testData(project(":compiler").isolated, "testData/loadJava")
    testData(project(":compiler").isolated, "testData/klib/partial-linkage")
    testData(project(":compiler").isolated, "testData/klib/resolve")
    testData(project(":compiler").isolated, "testData/klib/syntheticAccessors")
    testData(project(":compiler").isolated, "testData/klib/__utils__")

    testData(project(":js:js.translator").isolated, "testData/_commonFiles")
    testData(project(":js:js.translator").isolated, "testData/moduleEmulation.js")
    testData(project(":js:js.translator").isolated, "testData/incremental")
    testData(project(":js:js.translator").isolated, "testData/box")
    testData(project(":js:js.translator").isolated, "testData/lineNumbers")
    testData(project(":js:js.translator").isolated, "testData/js-optimizer/")
    testData(project(":js:js.translator").isolated, "testData/js-name-resolution")
    testData(project(":js:js.translator").isolated, "testData/multiModuleOrder")
    testData(project(":js:js.translator").isolated, "testData/sourcemap")
    testData(project(":js:js.translator").isolated, "testData/typescript-export/js/")
    testData(project(":compiler").isolated, "testData/debug/stepping")
    testData(project(":compiler").isolated, "testData/debug/localVariables")

    testGenerator(
        "org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt",
        generateTestsInBuildDirectory = true,
    )

    withJsRuntime()
    withStdlibWeb()
    withStdlibCommon()
    withWasmRuntime()
}

testsJar {}

val testJsFile = testDataDir.resolve("test.js")
val packageJsonFile = testDataDir.resolve("package.json")
val packageLockJsonFile = testDataDir.resolve("package-lock.json")

val prepareNpmTestData = tasks.register<Copy>("prepareNpmTestNpmData") {
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

tasks.processTestFixturesResources.configure {
    from(project.layout.projectDirectory.dir("_additionalFilesForTests"))
    from(project(":compiler").isolated.projectDirectory.dir("testData/debug")) {
        into("debugTestHelpers")
        include("jsTestHelpers/")
    }
}
