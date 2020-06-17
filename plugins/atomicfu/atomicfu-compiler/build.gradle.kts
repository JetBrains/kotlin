import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.gradle.internal.os.OperatingSystem


description = "Atomicfu Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.github.node-gradle.node") version "2.2.0"
    id("de.undercouch.download")
    id("com.gradle.enterprise.test-distribution")
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

val atomicfuClasspath by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val atomicfuRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
    }
}

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.translator"))
    compile(project(":compiler:backend.js"))

    runtimeOnly(kotlinStdlib())

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":js:js.tests"))
    testCompile(commonDep("junit:junit"))

    testRuntime(kotlinStdlib())
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-preloader")) // it's required for ant tests
    testRuntime(project(":compiler:backend-common"))
    testRuntime(commonDep("org.fusesource.jansi", "jansi"))

    atomicfuClasspath("org.jetbrains.kotlinx:atomicfu-js:0.15.1") {
        isTransitive = false
    }

    atomicfuRuntimeForTests(project(":kotlinx-atomicfu-runtime"))  { isTransitive = false }

    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.6.2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(atomicfuRuntimeForTests)
    doFirst {
        systemProperty("atomicfuRuntimeForTests.classpath", atomicfuRuntimeForTests.asPath)
    }
    setUpJsBoxTests(jsEnabled = true, jsIrEnabled = true)
}

enum class OsName { WINDOWS, MAC, LINUX, UNKNOWN }
enum class OsArch { X86_32, X86_64, UNKNOWN }
data class OsType(val name: OsName, val arch: OsArch)

fun Test.setupV8() {
    dependsOn(":js:js.tests:unzipV8")
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
    val v8osString = when (currentOsType) {
        OsType(OsName.LINUX, OsArch.X86_32) -> "linux32"
        OsType(OsName.LINUX, OsArch.X86_64) -> "linux64"
        OsType(OsName.MAC, OsArch.X86_64) -> "mac64"
        OsType(OsName.WINDOWS, OsArch.X86_32) -> "win32"
        OsType(OsName.WINDOWS, OsArch.X86_64) -> "win64"
        else -> error("unsupported os type $currentOsType")
    }
    val v8Path = "${rootDir.absolutePath}/js/js.tests/build/tools/v8-${v8osString}-rel-8.8.104/"
    val v8ExecutablePath = File(v8Path, "d8")
    systemProperty("javascript.engine.path.V8", v8ExecutablePath)
    inputs.dir(v8Path)
}

fun Test.setUpJsBoxTests(jsEnabled: Boolean, jsIrEnabled: Boolean) {
    setupV8()

    dependsOn(":dist")
    if (jsIrEnabled) {
        dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
        systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/js-ir/build/classes/kotlin/js/main")
        dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
        systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
        dependsOn(":kotlin-test:kotlin-test-js-ir:compileKotlinJs")
        systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
        systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
        systemProperty("kotlin.js.test.root.out.dir", "$buildDir/")
        systemProperty("atomicfu.classpath", atomicfuClasspath.asPath)
    }
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt")
val testDataDir = project(":js:js.translator").projectDir.resolve("testData")