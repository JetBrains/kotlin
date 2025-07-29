import org.gradle.internal.os.OperatingSystem
import java.net.URI
import java.util.*

plugins {
    kotlin("jvm")
    id("jps-compatible")
    alias(libs.plugins.gradle.node)
    id("d8-configuration")
    id("binaryen-configuration")
    id("nodejs-configuration")
    id("java-test-fixtures")
}

node {
    download.set(true)
    version.set(nodejsVersion)
    nodeProjectDir.set(layout.buildDirectory.dir("node"))
}

repositories {
    ivy {
        url = URI("https://archive.mozilla.org/pub/firefox/releases/")
        patternLayout {
            artifact("[revision]/jsshell/[artifact]-[classifier].[ext]")
        }
        metadataSources { artifact() }
        content { includeModule("org.mozilla", "jsshell") }
    }
    ivy {
        url = URI("https://github.com/WasmEdge/WasmEdge/releases/download/")
        patternLayout {
            artifact("[revision]/WasmEdge-[revision]-[classifier].[ext]")
        }
        metadataSources { artifact() }
        content { includeModule("org.wasmedge", "wasmedge") }
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

    val osArch = when (providers.systemProperty("sun.arch.data.model").get()) {
        "32" -> OsArch.X86_32
        "64" -> when (providers.systemProperty("os.arch").get().lowercase()) {
            "aarch64" -> OsArch.ARM64
            else -> OsArch.X86_64
        }
        else -> OsArch.UNKNOWN
    }

    OsType(osName, osArch)
}


val jsShellVersion = "134.0.2"
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
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
    testFixturesApi(testFixtures(project(":wasm:wasm.tests")))
    testFixturesApi(intellijCore())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    jsShell("org.mozilla:jsshell:$jsShellVersion:$jsShellSuffix@zip")

    implicitDependencies("org.mozilla:jsshell:$jsShellVersion:win64@zip")
    implicitDependencies("org.mozilla:jsshell:$jsShellVersion:linux-x86_64@zip")
    implicitDependencies("org.mozilla:jsshell:$jsShellVersion:mac@zip")

}

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

fun Test.setupWasmStdlib(target: String) {
    @Suppress("LocalVariableName")
    val Target = target.capitalize()
    dependsOn(":kotlin-stdlib:compileKotlinWasm$Target")
    systemProperty("kotlin.wasm-$target.stdlib.path", "libraries/stdlib/build/classes/kotlin/wasm$Target/main")
    dependsOn(":kotlin-test:compileKotlinWasm$Target")
    systemProperty("kotlin.wasm-$target.kotlin.test.path", "libraries/kotlin.test/build/classes/kotlin/wasm$Target/main")
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

val unzipJsShell by task<Copy> {
    dependsOn(jsShell)
    from {
        zipTree(jsShell.singleFile)
    }
    into(layout.buildDirectory.dir("tools/jsshell-$jsShellSuffix-$jsShellVersion"))
}

fun Test.setupSpiderMonkey() {
    dependsOn(unzipJsShell)
    val jsShellExecutablePath = File(unzipJsShell.get().destinationDir, "js").absolutePath
    systemProperty("javascript.engine.path.SpiderMonkey", jsShellExecutablePath)
}

val customCompilerVersion = findProperty("kotlin.internal.wasm.test.compat.customCompilerVersion") as String
val customCompilerArtifacts: Configuration by configurations.creating

dependencies {
    customCompilerArtifacts("org.jetbrains.kotlin:kotlin-compiler-embeddable:$customCompilerVersion")
    customCompilerArtifacts("org.jetbrains.kotlin:kotlin-stdlib-wasm-js:$customCompilerVersion")
    customCompilerArtifacts("org.jetbrains.kotlin:kotlin-test-wasm-js:$customCompilerVersion")
}

val customCompilerArtifactsDir: Provider<Directory> = layout.buildDirectory.dir("customCompiler$customCompilerVersion")

val downloadCustomCompilerArtifacts: TaskProvider<Sync> by tasks.registering(Sync::class) {
    from(customCompilerArtifacts)
    into(customCompilerArtifactsDir)
}

testsJar {}

fun Test.setUpWasmJsBoxTests() {
    workingDir = rootDir
    with(d8KotlinBuild) {
        setupV8()
    }
    with(nodeJsKotlinBuild) {
        setupNodeJs()
    }
    with(binaryenKotlinBuild) {
        setupBinaryen()
    }
    setupSpiderMonkey()
    setupWasmStdlib("js")
    setupGradlePropertiesForwarding()
    systemProperty("kotlin.wasm.test.root.out.dir", "${layout.buildDirectory.get().asFile}/")
}

fun Test.setUpCustomCompiler() {
    dependsOn(downloadCustomCompilerArtifacts)
    systemProperty("kotlin.internal.wasm.test.compat.customCompilerArtifactsDir", customCompilerArtifactsDir.get().asFile.absolutePath)
    systemProperty("kotlin.internal.wasm.test.compat.customCompilerVersion", customCompilerVersion)
}

projectTest(
    taskName = "testCustomFirstPhase",
    jUnitMode = JUnitMode.JUnit5,
) {
    setUpWasmJsBoxTests()
    setUpCustomCompiler()
    useJUnitPlatform { includeTags("custom-first-phase") }
}

@Suppress("unused")
val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateWasmKlibCompatibilityTestsKt") {
    dependsOn(":compiler:generateTestData")
}
