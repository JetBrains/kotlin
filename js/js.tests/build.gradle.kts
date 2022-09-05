import com.github.gradle.node.npm.task.NpmTask
import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin
import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("jps-compatible")
    id("com.github.node-gradle.node") version "3.2.1"
    id("de.undercouch.download")
}

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(buildDir)
}

val antLauncherJar by configurations.creating
val testJsRuntime by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}

dependencies {
    testApiJUnit5(vintageEngine = true)

    testApi(protobufFull())
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))

    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:cli-js"))
    testCompileOnly(project(":compiler:util"))
    testCompileOnly(intellijCore())
    testApi(project(":compiler:backend.js"))
    testApi(project(":compiler:backend.wasm"))
    testApi(project(":js:js.translator"))
    testApi(project(":js:js.serializer"))
    testApi(project(":js:js.dce"))
    testApi(project(":js:js.engines"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testApi(commonDependency("junit:junit"))
    testApi(projectTests(":kotlin-build-common"))
    testApi(projectTests(":generators:test-generator"))

    testApi(intellijCore())
    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:util"))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    testRuntimeOnly(commonDependency("com.google.guava:guava"))
    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps:jdom"))

    testRuntimeOnly(kotlinStdlib())
    testJsRuntime(kotlinStdlib("js"))
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testJsRuntime(project(":kotlin-test:kotlin-test-js")) // to be sure that kotlin-test-js built before tests runned
    }
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(project(":compiler:backend-common"))
    testRuntimeOnly(commonDependency("org.fusesource.jansi", "jansi"))
    
    antLauncherJar(commonDependency("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())

    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${commonDependencyVersion("org.junit", "junit-bom")}")

    testImplementation(commonDependency("org.jetbrains.kotlinx", "kotlinx-serialization-json"))
    testImplementation(commonDependency("io.ktor", "ktor-client-core"))
    testImplementation(commonDependency("io.ktor", "ktor-client-cio"))
    testImplementation(commonDependency("io.ktor", "ktor-client-websockets"))
}

val generationRoot = projectDir.resolve("tests-gen")

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

    val osArch = when (providers.systemProperty("sun.arch.data.model").forUseAtConfigurationTime().get()) {
        "32" -> OsArch.X86_32
        "64" -> when (providers.systemProperty("os.arch").forUseAtConfigurationTime().get().toLowerCase()) {
            "aarch64" -> OsArch.ARM64
            else -> OsArch.X86_64
        }
        else -> OsArch.UNKNOWN
    }

    OsType(osName, osArch)
}

val jsShellDirectory = "https://archive.mozilla.org/pub/firefox/nightly/2020/06/2020-06-29-15-46-04-mozilla-central"
val jsShellSuffix = when (currentOsType) {
    OsType(OsName.LINUX, OsArch.X86_32) -> "linux-i686"
    OsType(OsName.LINUX, OsArch.X86_64) -> "linux-x86_64"
    OsType(OsName.MAC, OsArch.X86_64),
    OsType(OsName.MAC, OsArch.ARM64) -> "mac"
    OsType(OsName.WINDOWS, OsArch.X86_32) -> "win32"
    OsType(OsName.WINDOWS, OsArch.X86_64) -> "win64"
    else -> error("unsupported os type $currentOsType")
}
val jsShellLocation = "$jsShellDirectory/jsshell-$jsShellSuffix.zip"

val downloadedTools = File(buildDir, "tools")

val downloadJsShell by task<Download> {
    src(jsShellLocation)
    dest(File(downloadedTools, "jsshell-$jsShellSuffix.zip"))
    overwrite(false)
}

val unzipJsShell by task<Copy> {
    dependsOn(downloadJsShell)
    from(zipTree(downloadJsShell.get().dest))
    val unpackedDir = File(downloadedTools, "jsshell-$jsShellSuffix")
    into(unpackedDir)
}

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")
val typescriptTestsDir = testDataDir.resolve("typescript-export")

val generateJsExportOnFileTestFilesForTS by task<Copy> {
    val exportFileDirPostfix = "-in-exported-file"

    from(typescriptTestsDir) {
        include("**/*.kt")
        include("**/*.ts")
        include("**/tsconfig.json")
        exclude("selective-export/*")
        exclude("implicit-export/*")
        exclude("inheritance/*")
        exclude("strict-implicit-export/*")
        exclude("*$exportFileDirPostfix")

        eachFile {
            path = "${relativePath.parent}$exportFileDirPostfix/$name"

            var isFirstLine = true
            filter {
                when {
                    isFirstLine && name.endsWith(".kt") -> "/** This file is generated by {@link :js:js.test:generateJsExportOnFileTestFilesForTS} task. DO NOT MODIFY MANUALLY */\n\n$it"
                        .also { isFirstLine = false }

                    it.contains("// FILE") -> "$it\n\n@file:JsExport"
                    else -> it.replace("@JsExport", "")
                }
            }

        }

    }

    into(typescriptTestsDir)
}


val installTsDependencies = task<NpmTask>("installTsDependencies") {
    workingDir.set(testDataDir)
    args.set(listOf("install"))
}

fun sequential(first: Task, tasks: List<Task>): Task {
    tasks.fold(first) { previousTask, currentTask ->
        currentTask.dependsOn(previousTask)
    }
    return tasks.last()
}

fun generateTypeScriptTestFor(dir: String): Task = task<NpmTask>("generate-ts-for-$dir") {
    val baseDir = fileTree(testDataDir.resolve("./typescript-export/$dir"))

    workingDir.set(testDataDir)
    inputs.files(baseDir.include("*.ts"))
    outputs.files(baseDir.include("*.js"))
    args.set(listOf("run", "generateTypeScriptTests", "--", "./typescript-export/$dir/tsconfig.json"))
}

val generateTypeScriptTests = sequential(
    installTsDependencies,
    typescriptTestsDir.listFiles()
        .filter { it.isDirectory }
        .map { generateTypeScriptTestFor(it.name) }
)

fun Test.setupNodeJs() {
    systemProperty("javascript.engine.path.NodeJs", com.github.gradle.node.variant.VariantComputer()
        .let { variantComputer ->
            variantComputer
                .computeNodeDir(node)
                .let { variantComputer.computeNodeBinDir(it) }
                .let { variantComputer.computeNodeExec(node, it) }
                .get()
        }
    )
}

fun Test.setupSpiderMonkey() {
    dependsOn(unzipJsShell)
    val jsShellExecutablePath = File(unzipJsShell.get().destinationDir, "js").absolutePath
    systemProperty("javascript.engine.path.SpiderMonkey", jsShellExecutablePath)
}

val d8Plugin = D8RootPlugin.apply(rootProject)
d8Plugin.version = v8Version

fun Test.setupV8() {
    dependsOn(d8Plugin.setupTaskProvider)
    doFirst {
        systemProperty("javascript.engine.path.V8", d8Plugin.requireConfigured().executablePath.absolutePath)
    }
}

fun Test.setUpJsBoxTests(jsEnabled: Boolean, jsIrEnabled: Boolean) {
    setupV8()
    if (jsIrEnabled)
        setupNodeJs()

    inputs.files(rootDir.resolve("js/js.engines/src/org/jetbrains/kotlin/js/engine/repl.js"))

    dependsOn(":dist")

    if (!project.hasProperty("teamcity")) {
        dependsOn(generateTypeScriptTests)
    }

    if (jsEnabled) {
        dependsOn(testJsRuntime)
        inputs.files(testJsRuntime)
    }

    if (jsIrEnabled) {
        dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
        systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/js-ir/build/classes/kotlin/js/main")
        inputs.dir(rootDir.resolve("libraries/stdlib/js-ir/build/classes/kotlin/js/main"))

        dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
        systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
        inputs.dir(rootDir.resolve("libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main"))

        dependsOn(":kotlin-test:kotlin-test-js-ir:compileKotlinJs")
        systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
        inputs.dir(rootDir.resolve("libraries/kotlin.test/js-ir/build/classes/kotlin/js/main"))
    }

    exclude("org/jetbrains/kotlin/js/testOld/wasm/semantics/*")

    if (jsEnabled && !jsIrEnabled) exclude("org/jetbrains/kotlin/js/test/ir/*")
    if (!jsEnabled && jsIrEnabled) include("org/jetbrains/kotlin/js/test/ir/*")

    jvmArgs("-da:jdk.nashorn.internal.runtime.RecompilableScriptFunctionData") // Disable assertion which fails due to a bug in nashorn (KT-23637)
    setUpBoxTests()
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

    systemProperty("kotlin.js.test.root.out.dir", "$buildDir/")
    systemProperty("overwrite.output", project.providers.gradleProperty("overwrite.output")
        .forUseAtConfigurationTime().orNull ?: "false")

    val prefixForPpropertiesToForward = "fd."
    for ((key, value) in properties) {
        if (key.startsWith(prefixForPpropertiesToForward)) {
            systemProperty(key.substring(prefixForPpropertiesToForward.length), value!!)
        }
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5, maxHeapSizeMb = 4096) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = true)

    inputs.dir(rootDir.resolve("compiler/cli/cli-common/resources")) // compiler.xml

    inputs.dir(testDataDir)
    inputs.dir(rootDir.resolve("dist"))
    inputs.dir(rootDir.resolve("compiler/testData"))
    inputs.dir(rootDir.resolve("libraries/stdlib/api/js"))
    inputs.dir(rootDir.resolve("libraries/stdlib/api/js-v1"))

    outputs.dir("$buildDir/out")
    outputs.dir("$buildDir/out-min")

    systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/js-ir/build/libs/kotlin-stdlib-js-ir-js-$version.klib")

    configureTestDistribution()
}

projectTest("jsTest", parallel = true, jUnitMode = JUnitMode.JUnit5, maxHeapSizeMb = 4096) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = false)
    useJUnitPlatform()
}

projectTest("jsIrTest", true, jUnitMode = JUnitMode.JUnit5, maxHeapSizeMb = 4096) {
    setUpJsBoxTests(jsEnabled = false, jsIrEnabled = true)
    useJUnitPlatform()
}

projectTest("quickTest", parallel = true, jUnitMode = JUnitMode.JUnit5, maxHeapSizeMb = 4096) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = false)
    systemProperty("kotlin.js.skipMinificationTest", "true")
    useJUnitPlatform()
}

testsJar {}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt") {
    dependsOn(":compiler:generateTestData")
    dependsOn(generateJsExportOnFileTestFilesForTS)
}

val prepareMochaTestData by tasks.registering(Copy::class) {
    from(testDataDir) {
        include("package.json")
        include("test.js")
    }
    into(buildDir)
}

val npmInstall by tasks.getting(NpmTask::class) {
    dependsOn(prepareMochaTestData)
    workingDir.set(buildDir)
}

val runMocha by task<NpmTask> {
    workingDir.set(buildDir)

    val target = if (project.hasProperty("teamcity")) "runOnTeamcity" else "test"
    args.set(listOf("run", target))

    ignoreExitValue.set(kotlinBuildProperties.ignoreTestFailures)

    dependsOn(npmInstall, "test")

    val check by tasks
    check.dependsOn(this)

    environment.set(mapOf(
        "KOTLIN_JS_LOCATION" to rootDir.resolve("dist/js/kotlin.js").toString(),
        "KOTLIN_JS_TEST_LOCATION" to rootDir.resolve("dist/js/kotlin-test.js").toString(),
        "BOX_FLAG_LOCATION" to rootDir.resolve("compiler/testData/jsBoxFlag.js").toString()
    ))
}

projectTest("wasmTest", true) {
    setupV8()
    setupSpiderMonkey()

    include("org/jetbrains/kotlin/js/testOld/wasm/semantics/*")

    dependsOn(":kotlin-stdlib-wasm:compileKotlinWasm")
    systemProperty("kotlin.wasm.stdlib.path", "libraries/stdlib/wasm/build/classes/kotlin/wasm/main")

    dependsOn(":kotlin-test:kotlin-test-wasm:compileKotlinWasm")
    systemProperty("kotlin.wasm.kotlin.test.path", "libraries/kotlin.test/wasm/build/classes/kotlin/wasm/main")

    setUpBoxTests()
}

projectTest("invalidationTest", jUnitMode = JUnitMode.JUnit4) {
    setupV8()
    workingDir = rootDir

    include("org/jetbrains/kotlin/incremental/*")

    dependsOn(":dist")
    dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")

    systemProperty("kotlin.js.stdlib.klib.path", "libraries/stdlib/js-ir/build/libs/kotlin-stdlib-js-ir-js-$version.klib")
}
