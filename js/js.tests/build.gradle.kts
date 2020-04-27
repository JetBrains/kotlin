import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.npm.NpmTask
import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.github.node-gradle.node") version "2.2.0"
    id("de.undercouch.download")
}

node {
    download = true
    version = "10.16.2"
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
    Platform[193].orLower {
        testCompileOnly(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }
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

    if (Platform[193].orLower()) {
        testRuntime(intellijDep()) { includeJars("picocontainer", rootProject = rootProject) }
    }
    testRuntime(intellijDep()) { includeJars("trove4j", "guava", "jdom", rootProject = rootProject) }


    val currentOs = OperatingSystem.current()

    val j2v8idString = when {
        currentOs.isWindows -> {
            val suffix = if (currentOs.toString().endsWith("64")) "_64" else ""
            "com.eclipsesource.j2v8:j2v8_win32_x86$suffix:4.6.0"
        }
        currentOs.isMacOsX -> "com.eclipsesource.j2v8:j2v8_macosx_x86_64:4.6.0"
        currentOs.run { isLinux || isUnix } -> "com.eclipsesource.j2v8:j2v8_linux_x86_64:4.8.0"
        else -> {
            logger.error("unsupported platform $currentOs - can not compile com.eclipsesource.j2v8 dependency")
            "j2v8:$currentOs"
        }
    }

    testCompileOnly("com.eclipsesource.j2v8:j2v8_linux_x86_64:4.8.0")
    testRuntimeOnly(j2v8idString)

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
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}


fun Test.setUpJsBoxTests(jsEnabled: Boolean, jsIrEnabled: Boolean) {
    dependsOn(":dist")
    if (jsEnabled) dependsOn(testJsRuntime)
    if (jsIrEnabled) {
        dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
        systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/js-ir/build/classes/kotlin/js/main")
        dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
        systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
        dependsOn(":kotlin-test:kotlin-test-js-ir:compileKotlinJs")
        systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
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
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJar.asPath)
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
    }

    systemProperty("overwrite.output", findProperty("overwrite.output") ?: "false")

    val prefixForPpropertiesToForward = "fd."
    for ((key, value) in properties) {
        if (key.startsWith(prefixForPpropertiesToForward)) {
            systemProperty(key.substring(prefixForPpropertiesToForward.length), value!!)
        }
    }
}

projectTest(parallel = true) {
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = true)
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

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt")
val testDataDir = project(":js:js.translator").projectDir.resolve("testData")

extensions.getByType(NodeExtension::class.java).nodeModulesDir = testDataDir

val npmInstall by tasks.getting(NpmTask::class) {
    setWorkingDir(testDataDir)
}

val runMocha by task<NpmTask> {
    setWorkingDir(testDataDir)

    val target = if (project.hasProperty("teamcity")) "runOnTeamcity" else "test"
    setArgs(listOf("run", target))

    setIgnoreExitValue(kotlinBuildProperties.ignoreTestFailures)

    dependsOn(npmInstall, "test")

    val check by tasks
    check.dependsOn(this)
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

    val osArch = when (System.getProperty("sun.arch.data.model")) {
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

projectTest("wasmTest", true) {
    dependsOn(unzipJsShell)
    dependsOn(unzipV8)
    include("org/jetbrains/kotlin/js/test/wasm/semantics/*")
    val jsShellExecutablePath = File(unzipJsShell.get().destinationDir, "js").absolutePath
    val v8ExecutablePath = File(unzipV8.get().destinationDir, "d8").absolutePath
    println(v8ExecutablePath)

    systemProperty("javascript.engine.path.SpiderMonkey", jsShellExecutablePath)
    systemProperty("javascript.engine.path.V8", v8ExecutablePath)

    dependsOn(":kotlin-stdlib-wasm:compileKotlinJs")
    systemProperty("kotlin.wasm.stdlib.path", "libraries/stdlib/wasm/build/classes/kotlin/js/main")

    setUpBoxTests()
}
