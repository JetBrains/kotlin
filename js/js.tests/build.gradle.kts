import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.variant.computeNodeExec
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import java.util.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("jps-compatible")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
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
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testJsRuntime(kotlinTest("js")) // to be sure that kotlin-test-js built before tests run
    }
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
    testFixturesCompileOnly(libs.ktor.client.cio)
    testFixturesCompileOnly(libs.ktor.client.core)
    testFixturesCompileOnly(libs.ktor.client.websockets)
    testRuntimeOnly(libs.kotlinx.serialization.json)
    testRuntimeOnly(libs.ktor.client.cio)
    testRuntimeOnly(libs.ktor.client.core)
    testRuntimeOnly(libs.ktor.client.websockets)
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        generatedTestDir()
    }
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

fun generateTypeScriptTestFor(dir: String): TaskProvider<NpmTask> = tasks.register<NpmTask>("generate-ts-for-$dir") {
    val baseDir = jsTestsDir.resolve(dir)
    val mainTsFile = fileTree(baseDir).files.find {
        it.name.endsWith("__main.ts") || it.name.endsWith("__main.mts")
    } ?: return@register

    val mainJsFile = baseDir.resolve("${mainTsFile.nameWithoutExtension}.js")
    val mainMjsFile = baseDir.resolve("${mainTsFile.nameWithoutExtension}.mjs")

    workingDir.set(testDataDir)

    inputs.file(mainTsFile)
    outputs.file(mainJsFile)
    outputs.file(mainMjsFile)
    outputs.upToDateWhen {
        mainJsFile.exists() || mainMjsFile.exists()
    }

    args.set(listOf("run", "generateTypeScriptTests", "--", "./typescript-export/js/$dir/tsconfig.json"))
}

val generateTypeScriptTests by parallel(
    beforeAll = installTsDependencies,
    tasksToRun = jsTestsDir.listFiles { it: File ->
        it.isDirectory &&
                !it.path.endsWith("module-systems") &&
                !it.path.endsWith("module-systems-in-exported-file")
    }
        .map { generateTypeScriptTestFor(it.name) }
)

fun Test.setupNodeJs() {
    systemProperty(
        "javascript.engine.path.NodeJs",
        com.github.gradle.node.variant.VariantComputer()
            .let { variantComputer ->
                computeNodeExec(node, variantComputer.computeNodeBinDir(node.resolvedNodeDir, node.resolvedPlatform)).get()
            }
    )
}

fun Test.setUpJsBoxTests(tags: String?) {
    with(d8KotlinBuild) {
        setupV8()
    }

    setupNodeJs()
    dependsOn(npmInstall)

    inputs.files(rootDir.resolve("js/js.tests/testFixtures/org/jetbrains/kotlin/js/engine/repl.js"))

    dependsOn(":dist")
    dependsOn(generateTypeScriptTests)

    dependsOn(":kotlin-stdlib:jsJar")
    systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/build/classes/kotlin/js/main")
    inputs.dir(rootDir.resolve("libraries/stdlib/build/classes/kotlin/js/main"))

    systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib")
    inputs.file(rootDir.resolve("libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib"))

    dependsOn(":kotlin-stdlib:compileKotlinWasmJs")
    systemProperty("kotlin.wasm.full.stdlib.path", "libraries/stdlib/build/classes/kotlin/wasmJs/main")
    inputs.dir(rootDir.resolve("libraries/stdlib/build/classes/kotlin/wasmJs/main"))

    dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
    systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
    inputs.dir(rootDir.resolve("libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main"))

    dependsOn(":kotlin-test:jsJar")
    systemProperty("kotlin.js.kotlin.test.klib.path", "libraries/kotlin.test/build/libs/kotlin-test-js-$version.klib")
    inputs.file(rootDir.resolve("libraries/kotlin.test/build/libs/kotlin-test-js-$version.klib"))

    systemProperty("kotlin.js.full.test.path", "libraries/kotlin.test/build/classes/kotlin/js/main")
    inputs.dir(rootDir.resolve("libraries/kotlin.test/build/classes/kotlin/js/main"))

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
    workingDir = rootDir
    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")
    systemProperty(
        "overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: "false"
    )

    forwardProperties()
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        setUpJsBoxTests(null)

        inputs.dir(rootDir.resolve("compiler/cli/cli-common/resources")) // compiler.xml

        inputs.dir(testDataDir)
        inputs.dir(rootDir.resolve("dist"))
        inputs.dir(rootDir.resolve("compiler/testData"))

        outputs.dir(layout.buildDirectory.dir("out"))
        outputs.dir(layout.buildDirectory.dir("out-min"))

        configureTestDistribution()
    }

    testTask("jsTest", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        setUpJsBoxTests("!es6")
    }

    testTask("jsES6Test", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        setUpJsBoxTests("es6")
    }

    testTask("jsFirES6Test", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        // TODO(KTI-2710): Drop this task when we reconfigure TeamCity to run `jsES6Test`
        setUpJsBoxTests("es6")
    }

    testTask("invalidationTest", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = true) {
        workingDir = rootDir

        useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)
        include("org/jetbrains/kotlin/incremental/*")
        dependsOn(":dist")
        forwardProperties()
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt")
}

testsJar {}

val testJsFile = testDataDir.resolve("test.js")
val packageJsonFile = testDataDir.resolve("package.json")
val packageLockJsonFile = testDataDir.resolve("package-lock.json")

val prepareNpmTestData by task<Copy> {
    inputs.files(testJsFile, packageJsonFile, packageLockJsonFile)

    from(testJsFile)
    from(packageJsonFile)
    from(packageLockJsonFile)
    into(node.nodeProjectDir)
}

val npmInstall by tasks.getting(NpmTask::class) {
    val packageLockFile = testDataDir.resolve("package-lock.json")

    inputs.file(node.nodeProjectDir.file("package.json"))
    inputs.file(packageLockFile)
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
