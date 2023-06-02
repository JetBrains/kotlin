import org.gradle.internal.os.OperatingSystem
import java.net.URI
import java.util.*

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    ivy {
        url = URI("https://archive.mozilla.org/pub/firefox/nightly/")
        patternLayout {
            artifact("2023/05/[revision]/[artifact]-[classifier].[ext]")
        }
        metadataSources { artifact() }
        content { includeModule("org.mozilla", "jsshell") }
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


val jsShellVersion = "2023-05-12-09-49-14-mozilla-central"
val jsShellSuffix = when (currentOsType) {
    OsType(OsName.LINUX, OsArch.X86_32) -> "linux-i686"
    OsType(OsName.LINUX, OsArch.X86_64) -> "linux-x86_64"
    OsType(OsName.MAC, OsArch.X86_64),
    OsType(OsName.MAC, OsArch.ARM64) -> "mac"
    OsType(OsName.WINDOWS, OsArch.X86_32) -> "win32"
    OsType(OsName.WINDOWS, OsArch.X86_64) -> "win64"
    else -> error("unsupported os type $currentOsType")
}

val jsShell by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    testApi(commonDependency("junit:junit"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(intellijCore())

    jsShell("org.mozilla:jsshell:$jsShellVersion:$jsShellSuffix@zip")
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

fun Test.setupWasmStdlib() {
    dependsOn(":kotlin-stdlib-wasm:compileKotlinWasm")
    systemProperty("kotlin.wasm.stdlib.path", "libraries/stdlib/wasm/build/classes/kotlin/wasm/main")
    dependsOn(":kotlin-test:kotlin-test-wasm:compileKotlinWasm")
    systemProperty("kotlin.wasm.kotlin.test.path", "libraries/kotlin.test/wasm/build/classes/kotlin/wasm/main")
}

fun Test.setupGradlePropertiesForwarding() {
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

val downloadedTools = File(buildDir, "tools")

val unzipJsShell by task<Copy> {
    dependsOn(jsShell)
    from {
        zipTree(jsShell.singleFile)
    }
    val unpackedDir = File(downloadedTools, "jsshell-$jsShellSuffix-$jsShellVersion")
    into(unpackedDir)
}

fun Test.setupSpiderMonkey() {
    dependsOn(unzipJsShell)
    val jsShellExecutablePath = File(unzipJsShell.get().destinationDir, "js").absolutePath
    systemProperty("javascript.engine.path.SpiderMonkey", jsShellExecutablePath)
}

testsJar {}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateWasmTestsKt") {
    dependsOn(":compiler:generateTestData")
}

projectTest(parallel = true) {
    workingDir = rootDir
    exclude("**/diagnostics/*.class")
    setupV8()
    setupSpiderMonkey()
    setupWasmStdlib()
    setupGradlePropertiesForwarding()
    systemProperty("kotlin.wasm.test.root.out.dir", "$buildDir/")
}

projectTest(
    taskName = "diagnosticsTest",
    parallel = true,
    jUnitMode = JUnitMode.JUnit5
) {
    workingDir = rootDir
    include("**/diagnostics/*.class")
    useJUnitPlatform()
    setupWasmStdlib()
    setupGradlePropertiesForwarding()
    systemProperty("kotlin.wasm.test.root.out.dir", "$buildDir/")
}
