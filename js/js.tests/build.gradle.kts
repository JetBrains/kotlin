import com.github.gradle.node.npm.task.NpmTask
import com.github.gradle.node.variant.computeNodeExec
import org.apache.tools.ant.filters.FixCrLfFilter
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.ideaExt.idea
import java.util.*

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("jps-compatible")
    alias(libs.plugins.gradle.node)
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

val antLauncherJar by configurations.creating
val testJsRuntime by configurations.creating {
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

dependencies {
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)

    testApi(protobufFull())
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:fir:analysis-tests"))

    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:cli-js"))
    testCompileOnly(project(":compiler:util"))
    testCompileOnly(intellijCore())
    testApi(project(":compiler:backend.js"))
    testApi(project(":js:js.translator"))
    testApi(project(":js:js.serializer"))
    testApi(project(":js:js.dce"))
    testApi(project(":js:js.engines"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testImplementation(libs.junit4)
    testApi(projectTests(":kotlin-build-common"))
    testApi(projectTests(":generators:test-generator"))

    testApi(intellijCore())
    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:util"))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(libs.guava)
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:jdom"))

    testRuntimeOnly(kotlinStdlib())
    testJsRuntime(kotlinStdlib())
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testJsRuntime(kotlinTest("js")) // to be sure that kotlin-test-js built before tests run
    }
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(project(":compiler:backend-common"))
    testRuntimeOnly(commonDependency("org.fusesource.jansi", "jansi"))

    antLauncherJar(commonDependency("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())

    testRuntimeOnly(libs.junit.vintage.engine)

    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-serialization-json"))
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.websockets)
}

val generationRoot = projectDir.resolve("tests-gen")

useD8Plugin()
optInToExperimentalCompilerApi()

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

abstract class MochaTestTask : NpmTask(), VerificationTask

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")
val typescriptTestsDir = testDataDir.resolve("typescript-export")
val jsTestsDir = typescriptTestsDir.resolve("js")

val installTsDependencies by task<NpmTask> {
    val packageLockFile = testDataDir.resolve("package-lock.json")
    val nodeModules = testDataDir.resolve("node_modules")
    inputs.file(testDataDir.resolve("package.json"))
    outputs.file(packageLockFile)
    outputs.upToDateWhen { nodeModules.exists() }

    workingDir.set(testDataDir)
    args.set(listOf("install"))
}

val exportFileDirPostfix = "-in-exported-file"

fun generateJsExportOnFileTestFor(dir: String): TaskProvider<Copy> = tasks.register<Copy>("generate-js-export-on-file-for-$dir") {
    val dirPostfix = exportFileDirPostfix
    val inputDir = fileTree(jsTestsDir.resolve(dir))
    val outputDir = jsTestsDir.resolve("$dir$dirPostfix")

    inputs.files(inputDir.matching {
        include("**/*.kt")
        include("**/*.ts")
        include("**/tsconfig.json")
    })
    outputs.dir(outputDir)

    from(inputDir) {
        include("**/*.kt")
        include("**/*.ts")
        include("**/tsconfig.json")
    }

    eachFile {
        var isFirstLine = true

        filter {
            when {
                isFirstLine && name.endsWith(".kt") -> "/** This file is generated by {@link :js:js.test:generateTypeScriptJsExportOnFileTests} task. DO NOT MODIFY MANUALLY */\n\n$it"
                    .also { isFirstLine = false }

                it.contains("// FILE") -> "$it\n\n@file:JsExport"
                else -> it.replace("@JsExport(?!.)".toRegex(), "")
            }
        }

        filter(
            FixCrLfFilter::class,
            "eol" to FixCrLfFilter.CrLf.newInstance("lf"),
            "fixlast" to false
        )
    }

    into(outputDir)
}

fun generateTypeScriptTestFor(dir: String): TaskProvider<NpmTask> = tasks.register<NpmTask>("generate-ts-for-$dir") {
    val baseDir = jsTestsDir.resolve(dir)
    val mainTsFile = fileTree(baseDir).files.find { it.name.endsWith("__main.ts") } ?: return@register
    val mainJsFile = baseDir.resolve("${mainTsFile.nameWithoutExtension}.js")

    workingDir.set(testDataDir)

    inputs.file(mainTsFile)
    outputs.file(mainJsFile)
    outputs.upToDateWhen { mainJsFile.exists() }

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

val generateTypeScriptJsExportOnFileTests by parallel(
    jsTestsDir
        .listFiles { it: File ->
            it.isDirectory &&
                    !it.path.endsWith("selective-export") &&
                    !it.path.endsWith("implicit-export") &&
                    !it.path.endsWith("inheritance") &&
                    !it.path.endsWith("strict-implicit-export") &&
                    !it.path.endsWith("private-primary-constructor") &&
                    !it.path.endsWith(exportFileDirPostfix)
        }
        .map { generateJsExportOnFileTestFor(it.name) }
)

fun Test.setupNodeJs() {
    systemProperty("javascript.engine.path.NodeJs", com.github.gradle.node.variant.VariantComputer()
        .let { variantComputer ->
            computeNodeExec(node, variantComputer.computeNodeBinDir(node.resolvedNodeDir, node.resolvedPlatform)).get()
        }
    )
}

fun Test.setUpJsBoxTests(jsEnabled: Boolean, jsIrEnabled: Boolean, firEnabled: Boolean, es6Enabled: Boolean) {
    setupV8()

    if (jsIrEnabled) {
        setupNodeJs()
        dependsOn(npmInstall)
    }

    inputs.files(rootDir.resolve("js/js.engines/src/org/jetbrains/kotlin/js/engine/repl.js"))

    dependsOn(":dist")
    dependsOn(generateTypeScriptTests)

    if (jsEnabled) {
        dependsOn(testJsRuntime)
        inputs.files(testJsRuntime)
    }

    if (jsIrEnabled) {
        dependsOn(":kotlin-stdlib:jsJar")
        systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/build/classes/kotlin/js/main")
        inputs.dir(rootDir.resolve("libraries/stdlib/build/classes/kotlin/js/main"))

        systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib")
        inputs.file(rootDir.resolve("libraries/stdlib/build/libs/kotlin-stdlib-js-$version.klib"))

        dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
        systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
        inputs.dir(rootDir.resolve("libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main"))

        dependsOn(":kotlin-test:compileKotlinJs")
        systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/build/classes/kotlin/js/main")
        inputs.dir(rootDir.resolve("libraries/kotlin.test/build/classes/kotlin/js/main"))

        systemProperty("kotlin.js.kotlin.test.klib.path", "libraries/kotlin.test/build/libs/kotlin-test-js-$version.klib")
        inputs.file(rootDir.resolve("libraries/kotlin.test/build/libs/kotlin-test-js-$version.klib"))
    }

    exclude("org/jetbrains/kotlin/js/testOld/api/*")

    if (jsEnabled && !jsIrEnabled) {
        include("org/jetbrains/kotlin/integration/AntTaskJsTest.class")
        include("org/jetbrains/kotlin/js/testOld/*")
        include("org/jetbrains/kotlin/js/testOld/ast/*")
        include("org/jetbrains/kotlin/js/testOld/optimizer/*")
        include("org/jetbrains/kotlin/js/test/*")
    }
    if (!jsEnabled) {
        when {
            firEnabled && !es6Enabled -> {
                include("org/jetbrains/kotlin/js/test/fir/*")

                exclude("org/jetbrains/kotlin/js/test/fir/FirJsES6BoxTestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenBoxTestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenInlineTestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenBoxErrorTestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenWasmJsInteropTestGenerated.class")
            }
            es6Enabled -> {
                if (firEnabled) {
                    include("org/jetbrains/kotlin/js/test/fir/FirJsES6BoxTestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenBoxTestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenInlineTestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenBoxErrorTestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/fir/FirJsES6CodegenWasmJsInteropTestGenerated.class")
                } else {
                    include("org/jetbrains/kotlin/js/test/ir/IrBoxJsES6TestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/ir/IrJsES6CodegenBoxTestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/ir/IrJsES6CodegenInlineTestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/ir/IrJsES6CodegenBoxErrorTestGenerated.class")
                    include("org/jetbrains/kotlin/js/test/ir/IrJsES6TypeScriptExportTestGenerated.class")

                    include("org/jetbrains/kotlin/incremental/JsIrES6InvalidationTestGenerated.class")
                }
            }
            else -> {
                include("org/jetbrains/kotlin/js/test/ir/*")

                include("org/jetbrains/kotlin/incremental/*")
                include("org/jetbrains/kotlin/js/testOld/compatibility/binary/JsKlibBinaryCompatibilityTestGenerated.class")
                include("org/jetbrains/kotlin/benchmarks/GenerateIrRuntime.class")
                include("org/jetbrains/kotlin/integration/JsIrAnalysisHandlerExtensionTest.class")

                exclude("org/jetbrains/kotlin/js/test/ir/IrBoxJsES6TestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/ir/IrJsES6CodegenBoxTestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/ir/IrJsES6CodegenInlineTestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/ir/IrJsES6CodegenBoxErrorTestGenerated.class")
                exclude("org/jetbrains/kotlin/js/test/ir/IrJsES6TypeScriptExportTestGenerated.class")

                exclude("org/jetbrains/kotlin/incremental/JsIrES6InvalidationTestGenerated.class")
            }
        }
    }

    jvmArgs("-da:jdk.nashorn.internal.runtime.RecompilableScriptFunctionData") // Disable assertion which fails due to a bug in nashorn (KT-23637)
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
    dependsOn(antLauncherJar)
    inputs.files(antLauncherJar)
    val antLauncherJarPath = antLauncherJar.asPath
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJarPath)
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
    }

    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")
    systemProperty(
        "overwrite.output", project.providers.gradleProperty("overwrite.output").orNull ?: "false"
    )

    forwardProperties()
}

val test = projectTest(jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = true, firEnabled = true, es6Enabled = true)

    inputs.dir(rootDir.resolve("compiler/cli/cli-common/resources")) // compiler.xml

    inputs.dir(testDataDir)
    inputs.dir(rootDir.resolve("dist"))
    inputs.dir(rootDir.resolve("compiler/testData"))

    outputs.dir(layout.buildDirectory.dir("out"))
    outputs.dir(layout.buildDirectory.dir("out-min"))

    configureTestDistribution()
}

val jsTest = projectTest("jsTest", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = false, firEnabled = false, es6Enabled = false)
    useJUnitPlatform()
}

projectTest("jsIrTest", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests(jsEnabled = false, jsIrEnabled = true, firEnabled = false, es6Enabled = false)
    useJUnitPlatform()
}

projectTest("jsIrES6Test", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests(jsEnabled = false, jsIrEnabled = true, firEnabled = false, es6Enabled = true)
    useJUnitPlatform()
}

projectTest("jsFirTest", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests(jsEnabled = false, jsIrEnabled = true, firEnabled = true, es6Enabled = false)
    useJUnitPlatform()
}

projectTest("jsFirES6Test", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests(jsEnabled = false, jsIrEnabled = true, firEnabled = true, es6Enabled = true)
    useJUnitPlatform()
}

projectTest("quickTest", jUnitMode = JUnitMode.JUnit5) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = false, firEnabled = false, es6Enabled = false)
    systemProperty("kotlin.js.skipMinificationTest", "true")
    useJUnitPlatform()
}

projectTest("jsStdlibApiTest", parallel = true, maxHeapSizeMb = 4096) {
    setupV8()
    setupNodeJs()
    dependsOn(npmInstall)

    dependsOn(":dist")
    inputs.dir(rootDir.resolve("dist"))

    include("org/jetbrains/kotlin/js/testOld/api/*")
    inputs.dir(rootDir.resolve("libraries/stdlib/api/js"))

    dependsOn(":kotlin-stdlib:jsJar")
    systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/build/classes/kotlin/js/main")
    inputs.dir(rootDir.resolve("libraries/stdlib/build/classes/kotlin/js/main"))

    setTestNameIncludePatterns(listOf("org.jetbrains.kotlin.js.testOld.api.ApiTest.*"))

    setUpBoxTests()
}

testsJar {}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt") {
    dependsOn(":compiler:generateTestData")
    dependsOn(generateTypeScriptJsExportOnFileTests)
}


val testJsFile = testDataDir.resolve("test.js")
val packageJsonFile = testDataDir.resolve("package.json")

val prepareNpmTestData by task<Copy> {
    inputs.files(testJsFile, packageJsonFile)

    from(testJsFile)
    from(packageJsonFile)
    into(node.nodeProjectDir)
}

val npmInstall by tasks.getting(NpmTask::class) {
    val packageLockFile = testDataDir.resolve("package-lock.json")

    inputs.file(node.nodeProjectDir.file("package.json"))
    outputs.file(packageLockFile)
    outputs.upToDateWhen { packageLockFile.exists() }

    workingDir.fileProvider(node.nodeProjectDir.asFile)
    dependsOn(prepareNpmTestData)
}

val mochaTest by task<MochaTestTask> {
    workingDir.fileProvider(node.nodeProjectDir.asFile)

    val target = if (project.hasProperty("teamcity")) "runOnTeamcity" else "test"
    args.set(listOf("run", target))

    ignoreExitValue.set(kotlinBuildProperties.ignoreTestFailures)

    dependsOn(npmInstall)

    environment.set(
        mapOf(
            "KOTLIN_JS_LOCATION" to rootDir.resolve("dist/js/kotlin.js").toString(),
            "KOTLIN_JS_TEST_LOCATION" to rootDir.resolve("dist/js/kotlin-test.js").toString(),
            "BOX_FLAG_LOCATION" to rootDir.resolve("compiler/testData/jsBoxFlag.js").toString()
        )
    )
}

val runMocha by tasks.registering {
    dependsOn(jsTest)
    finalizedBy(mochaTest)
}

projectTest("invalidationTest", jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir

    useJsIrBoxTests(version = version, buildDir = layout.buildDirectory)
    include("org/jetbrains/kotlin/incremental/*")
    dependsOn(":dist")
    forwardProperties()
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn(runMocha)
}
