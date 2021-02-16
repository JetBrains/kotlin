import com.github.gradle.node.npm.task.NpmTask
import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.github.node-gradle.node") version "3.0.1"
    id("de.undercouch.download")
    id("com.gradle.enterprise.test-distribution") apply false
}

val isTeamcityBuild = project.kotlinBuildProperties.isTeamcityBuild
val testDistributionEnabled = project.providers.gradleProperty("kotlin.build.test.distribution.enabled")
    .forUseAtConfigurationTime().orNull?.toBoolean()
    ?: isTeamcityBuild

if (testDistributionEnabled) {
    apply<com.gradle.enterprise.gradleplugin.testdistribution.TestDistributionPlugin>()
}

node {
    download.set(true)
    version.set("10.16.2")
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
    testRuntime(intellijDep())

    testCompile(protobufFull())
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:cli-js"))
    testCompileOnly(project(":compiler:cli-js-klib"))
    testCompileOnly(project(":compiler:util"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt", "util") }
    testCompile(project(":compiler:backend.js"))
    testCompile(project(":compiler:backend.wasm"))
    testCompile(project(":js:js.translator"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.dce"))
    testCompile(project(":js:js.engines"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(projectTests(":generators:test-generator"))

    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler:util"))

    testRuntime(project(":kotlin-reflect"))

    testRuntime(intellijDep()) { includeJars("trove4j", "guava", "jdom", rootProject = rootProject) }

    testRuntime(kotlinStdlib())
    testJsRuntime(kotlinStdlib("js"))
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testJsRuntime(project(":kotlin-test:kotlin-test-js")) // to be sure that kotlin-test-js built before tests runned
    }
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-preloader")) // it's required for ant tests
    testRuntime(project(":compiler:backend-common"))
    testRuntime(commonDep("org.fusesource.jansi", "jansi"))
    
    antLauncherJar(commonDep("org.apache.ant", "ant"))
    antLauncherJar(toolsJar())

    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.6.2")
}

val generationRoot = projectDir.resolve("tests-gen")

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
enum class OsArch { X86_32, X86_64, UNKNOWN }
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
        "64" -> OsArch.X86_64
        else -> OsArch.UNKNOWN
    }

    OsType(osName, osArch)
}

val jsShellDirectory = "https://archive.mozilla.org/pub/firefox/nightly/2020/06/2020-06-29-15-46-04-mozilla-central"
val jsShellSuffix = when (currentOsType) {
    OsType(OsName.LINUX, OsArch.X86_32) -> "linux-i686"
    OsType(OsName.LINUX, OsArch.X86_64) -> "linux-x86_64"
    OsType(OsName.MAC, OsArch.X86_64) -> "mac"
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

val v8osString = when (currentOsType) {
    OsType(OsName.LINUX, OsArch.X86_32) -> "linux32"
    OsType(OsName.LINUX, OsArch.X86_64) -> "linux64"
    OsType(OsName.MAC, OsArch.X86_64) -> "mac64"
    OsType(OsName.WINDOWS, OsArch.X86_32) -> "win32"
    OsType(OsName.WINDOWS, OsArch.X86_64) -> "win64"
    else -> error("unsupported os type $currentOsType")
}

val v8edition = "rel" // rel or dbg
val v8version = "8.8.104"
val v8fileName = "v8-${v8osString}-${v8edition}-${v8version}"
val v8url = "https://storage.googleapis.com/chromium-v8/official/canary/$v8fileName.zip"

val downloadV8 by task<Download> {
    src(v8url)
    dest(File(downloadedTools, "$v8fileName.zip"))
    overwrite(false)
}

val unzipV8 by task<Copy> {
    dependsOn(downloadV8)
    from(zipTree(downloadV8.get().dest))
    val unpackedDir = File(downloadedTools, v8fileName)
    into(unpackedDir)
}

fun Test.setupV8() {
    dependsOn(unzipV8)
    val v8Path = unzipV8.get().destinationDir
    val v8ExecutablePath = File(v8Path, "d8")
    systemProperty("javascript.engine.path.V8", v8ExecutablePath)
    inputs.dir(v8Path)
}

fun Test.setupSpiderMonkey() {
    dependsOn(unzipJsShell)
    val jsShellExecutablePath = File(unzipJsShell.get().destinationDir, "js").absolutePath
    systemProperty("javascript.engine.path.SpiderMonkey", jsShellExecutablePath)
}

fun Test.setUpJsBoxTests(jsEnabled: Boolean, jsIrEnabled: Boolean) {
    setupV8()

    inputs.files(rootDir.resolve("js/js.engines/src/org/jetbrains/kotlin/js/engine/repl.js"))

    dependsOn(":dist")
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

    exclude("org/jetbrains/kotlin/js/test/wasm/semantics/*")
    exclude("org/jetbrains/kotlin/js/test/es6/semantics/*")

    if (jsEnabled && !jsIrEnabled) exclude("org/jetbrains/kotlin/js/test/ir/semantics/*")
    if (!jsEnabled && jsIrEnabled) include("org/jetbrains/kotlin/js/test/ir/semantics/*")

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

val testDataDir = project(":js:js.translator").projectDir.resolve("testData")

projectTest(parallel = true) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = true)

    inputs.dir(rootDir.resolve("compiler/cli/cli-common/resources")) // compiler.xml

    inputs.dir(testDataDir)
    inputs.dir(rootDir.resolve("dist"))
    inputs.dir(rootDir.resolve("compiler/testData"))
    inputs.dir(rootDir.resolve("libraries/stdlib/api/js"))
    inputs.dir(rootDir.resolve("libraries/stdlib/api/js-v1"))

    outputs.dir("$buildDir/out")
    outputs.dir("$buildDir/out-min")
    outputs.dir("$buildDir/out-pir")

    configureTestDistribution()
}

projectTest("jsTest", true) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = false)
}

projectTest("jsIrTest", true) {
    systemProperty("kotlin.js.ir.pir", "false")
    setUpJsBoxTests(jsEnabled = false, jsIrEnabled = true)
}

projectTest("jsEs6IrTest", true) {
    systemProperty("kotlin.js.ir.pir", "false")
    systemProperty("kotlin.js.ir.es6", "true")

    dependsOn(":dist")
    dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
    systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/js-ir/build/classes/kotlin/js/main")
    dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
    systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
    dependsOn(":kotlin-test:kotlin-test-js-ir:compileKotlinJs")
    systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")

    exclude("org/jetbrains/kotlin/js/test/wasm/semantics/*")
    exclude("org/jetbrains/kotlin/js/test/ir/semantics/*")
    exclude("org/jetbrains/kotlin/js/test/semantics/*")

    include("org/jetbrains/kotlin/js/test/es6/semantics/*")

    jvmArgs("-da:jdk.nashorn.internal.runtime.RecompilableScriptFunctionData") // Disable assertion which fails due to a bug in nashorn (KT-23637)
    setUpBoxTests()
}

projectTest("jsPirTest", true) {
    systemProperty("kotlin.js.ir.skipRegularMode", "true")
    setUpJsBoxTests(jsEnabled = false, jsIrEnabled = true)
}

projectTest("quickTest", true) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = false)
    systemProperty("kotlin.js.skipMinificationTest", "true")
}

testsJar {}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt") {
    dependsOn(":compiler:generateTestData")
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
        "KOTLIN_JS_TEST_LOCATION" to rootDir.resolve("dist/js/kotlin-test.js").toString()
    ))
}

projectTest("wasmTest", true) {
    setupV8()
    setupSpiderMonkey()

    include("org/jetbrains/kotlin/js/test/wasm/semantics/*")

    dependsOn(":kotlin-stdlib-wasm:compileKotlinJs")
    systemProperty("kotlin.wasm.stdlib.path", "libraries/stdlib/wasm/build/classes/kotlin/js/main")

    setUpBoxTests()
}
