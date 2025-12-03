import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import java.util.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

val cacheRedirectorEnabled = findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() == true

node {
    download.set(true)
    version.set(nodejsLtsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
    if (cacheRedirectorEnabled) {
        distBaseUrl.set("https://cache-redirector.jetbrains.com/nodejs.org/dist")
    }
}

val testJsRuntime by configurations.creating {
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
    testRuntimeOnly(libs.junit.vintage.engine)

    testFixturesApi(protobufFull())
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-compiler-utils")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesApi(testFixtures(project(":kotlin-util-klib")))

    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:cli-js"))
    testCompileOnly(project(":compiler:util"))
    testCompileOnly(intellijCore())
    testFixturesApi(project(":compiler:backend.js"))
    testFixturesApi(project(":js:js.translator"))
    testFixturesApi(project(":js:typescript-export-standalone"))
    testFixturesApi(project(":compiler:incremental-compilation-impl"))
    testImplementation(libs.junit4)
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

    testRuntimeOnly(libs.junit.vintage.engine)

    // these dependencies shouldn't be exposed to other modules
    // to avoid potential clashes in cases when another module
    // also needs one of these dependencies but of different
    // version (e.g. tests of kotlinx.serialization)
    testFixturesCompileOnly(libs.kotlinx.serialization.json)
    testRuntimeOnly(libs.kotlinx.serialization.json)
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()

sourceSets {
    "main" { }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")
val typescriptTestsDir = testDataDir.resolve("typescript-export")
val jsTestsDir = typescriptTestsDir.resolve("js")

val installTsDependencies by task<NpmTask> {
    val packageLockFile = testDataDir.resolve("package-lock.json")
    val nodeModules = testDataDir.resolve("node_modules")
    inputs.file(testDataDir.resolve("package.json"))
    inputs.file(packageLockFile)
    outputs.upToDateWhen { nodeModules.exists() }

    workingDir.set(testDataDir)
    npmCommand.set(listOf("ci"))
}

configurations.consumable("installedTSDependencies") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("npmrc"))
    }

    outgoing.artifact(
        installTsDependencies.map { it.outputs.files.singleFile }
    )
}

fun generateTypeScriptTestFor(dir: String): TaskProvider<NpmTask> = tasks.register<NpmTask>("generate-ts-for-$dir") {
    val baseDir = jsTestsDir.resolve(dir)
    val mainTsFile = fileTree(baseDir).files.find {
        it.name.endsWith("__main.ts") || it.name.endsWith("__main.mts")
    } ?: return@register

    val mainJsFile = baseDir.resolve("${mainTsFile.nameWithoutExtension}.js")
    val mainMjsFile = baseDir.resolve("${mainTsFile.nameWithoutExtension}.mjs")

    workingDir.set(testDataDir)

    // Inputs
    inputs.file(mainTsFile)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("mainTsFileToCompile")

    // Outputs
    outputs.file(mainJsFile)
    outputs.file(mainMjsFile)
    outputs.upToDateWhen { mainJsFile.exists() || mainMjsFile.exists() }

    args.set(listOf("run", "generateTypeScriptTests", "--", "./typescript-export/js/$dir/tsconfig.json"))
}

val generateTypeScriptTests = parallel(
    beforeAll = installTsDependencies,
    tasksToRun = jsTestsDir.listFiles { it: File ->
        it.isDirectory && !it.path.endsWith("module-systems")
    }
        .map { generateTypeScriptTestFor(it.name) }
)

fun Test.setUpJsBoxTests(tags: String?) {
    with(d8KotlinBuild) {
        setupV8()
    }

    dependsOn(npmInstall)

    jvmArgumentProviders += objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(rootDir.resolve("js/js.tests/testFixtures/org/jetbrains/kotlin/js/engine/repl.js"))
        property.set("javascript.engine.path.repl")
    }

    inputs.files(generateTypeScriptTests)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("compiledTypeScriptTestFiles")

    useJUnitPlatform {
        tags?.let { includeTags(it) }
    }

    setUpBoxTests()
}

fun Test.forwardProperties() {
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

fun Test.setUpBoxTests() {
    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")
    systemProperty(
        "overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: "false"
    )

    forwardProperties()
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        setUpJsBoxTests(null)
    }

    testTask("jsTest", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        setUpJsBoxTests("!es6")
    }

    testTask("jsES6Test", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        setUpJsBoxTests("es6")
    }

    testTask("invalidationTest", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)
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
        configureTestDataCollection = {
            inputs.files(generateTypeScriptTests)
                .withPathSensitivity(PathSensitivity.RELATIVE)
                .withPropertyName("compiledTypeScriptTestFiles")
        }
    )

    withJsRuntime()
    withStdlibCommon()
    withWasmRuntime()
}

testsJar {}

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

tasks.processTestFixturesResources.configure {
    from(project.layout.projectDirectory.dir("_additionalFilesForTests"))
    from(project(":compiler").layout.projectDirectory.dir("testData/debug")) {
        into("debugTestHelpers")
        include("jsTestHelpers/")
    }
}
