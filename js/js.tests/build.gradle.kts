import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.npm.NpmTask
import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.moowork.node").version("1.2.0")
}

node {
    download = true
}

val antLauncherJar by configurations.creating
val testJsRuntime by configurations.creating

dependencies {
    testRuntime(intellijDep())

    testCompile(protobufFull())
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:cli-js"))
    testCompileOnly(project(":compiler:util"))
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "idea_rt", "util") }
    testCompile(project(":compiler:backend.js"))
    testCompile(projectTests(":compiler:ir.serialization.js"))
    testCompile(project(":js:js.translator"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.dce"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(projectTests(":generators:test-generator"))

    testRuntime(kotlinStdlib())
    testJsRuntime(kotlinStdlib("js"))
    testJsRuntime(project(":kotlin-test:kotlin-test-js")) // to be sure that kotlin-test-js built before tests runned
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-preloader")) // it's required for ant tests
    testRuntime(project(":compiler:backend-common"))
    testRuntime(commonDep("org.fusesource.jansi", "jansi"))

    val currentOs = OperatingSystem.current()

    when {
        currentOs.isWindows -> {
            val suffix = if (currentOs.toString().endsWith("64")) "_64" else ""
            testCompile("com.eclipsesource.j2v8:j2v8_win32_x86$suffix:4.6.0")
        }
        currentOs.isMacOsX -> testCompile("com.eclipsesource.j2v8:j2v8_macosx_x86_64:4.6.0")
        currentOs.run { isLinux || isUnix } -> testCompile("com.eclipsesource.j2v8:j2v8_linux_x86_64:4.8.0")
        else -> logger.error("unsupported platform $currentOs - can not compile com.eclipsesource.j2v8 dependency")
    }
    
    antLauncherJar(commonDep("org.apache.ant", "ant"))
    antLauncherJar(files(toolsJar()))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}


fun Test.setUpBoxTests(jsEnabled: Boolean, jsIrEnabled: Boolean) {
    dependsOn(":dist")
    if (jsEnabled) dependsOn(testJsRuntime)
    if (jsIrEnabled) {
        dependsOn(":compiler:ir.serialization.js:generateFullRuntimeKLib")
        dependsOn(":compiler:ir.serialization.js:generateReducedRuntimeKLib")
        dependsOn(":compiler:ir.serialization.js:generateKotlinTestKLib")
    }

    if (jsEnabled && !jsIrEnabled) exclude("org/jetbrains/kotlin/js/test/ir/semantics/*")
    if (!jsEnabled && jsIrEnabled) include("org/jetbrains/kotlin/js/test/ir/semantics/*")

    jvmArgs("-da:jdk.nashorn.internal.runtime.RecompilableScriptFunctionData") // Disable assertion which fails due to a bug in nashorn (KT-23637)
    workingDir = rootDir
    if (findProperty("kotlin.compiler.js.ir.tests.skip")?.toString()?.toBoolean() == true) {
        exclude("org/jetbrains/kotlin/js/test/ir/semantics/*")
    }
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJar.asPath)
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
    }

    val prefixForPpropertiesToForward = "fd."
    for ((key, value) in properties) {
        if (key.startsWith(prefixForPpropertiesToForward)) {
            systemProperty(key.substring(prefixForPpropertiesToForward.length), value!!)
        }
    }
}

projectTest(parallel = true) {
    setUpBoxTests(jsEnabled = true, jsIrEnabled = true)
}

projectTest("jsTest", true) {
    setUpBoxTests(jsEnabled = true, jsIrEnabled = false)
}

projectTest("jsIrTest", true) {
    setUpBoxTests(jsEnabled = false, jsIrEnabled = true)
}

projectTest("quickTest", true) {
    setUpBoxTests(jsEnabled = true, jsIrEnabled = false)
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

    setIgnoreExitValue(rootProject.getBooleanProperty("ignoreTestFailures") ?: false)

    dependsOn(npmInstall, "test")

    val check by tasks
    check.dependsOn(this)
}
