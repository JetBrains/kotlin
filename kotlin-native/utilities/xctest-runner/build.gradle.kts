import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.bitcode.CompileToBitcodeExtension
import org.jetbrains.kotlin.cpp.CppUsage
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.konan.target.*
import java.io.ByteArrayOutputStream
import java.util.*

plugins {
    id("kotlin.native.build-tools-conventions")
    kotlin("multiplatform")
    id("compile-to-bitcode")
}

group = "org.jetbrains.kotlin.native.test.xctest"

val distDir: File by project
val konanHome: String by extra(distDir.absolutePath)
// Set native home for KGP
extra["kotlin.native.home"] = konanHome

with(PlatformInfo) {
    if (isMac()) {
        checkXcodeVersion(project)
    }
}

/**
 * Path to the target SDK platform.
 *
 * By default, K/N includes only SDK frameworks.
 * It's required to get the Library frameworks path where the `XCTest.framework` is located.
 */
fun targetPlatform(target: String): String {
    val out = ByteArrayOutputStream()
    val result = project.exec {
        executable = "/usr/bin/xcrun"
        args = listOf("--sdk", target, "--show-sdk-platform-path")
        standardOutput = out
    }

    check(result.exitValue == 0) {
        "xcrun ended unsuccessfully. See the output: $out"
    }

    return out.toString().trim()
}

val targets = listOf(
    KonanTarget.MACOS_X64,
    KonanTarget.MACOS_ARM64,
    KonanTarget.IOS_X64,
    KonanTarget.IOS_SIMULATOR_ARM64,
    KonanTarget.IOS_ARM64
).filter {
    it in platformManager.enabled
}

/*
 * Double laziness: lazily create functions that execute `/usr/bin/xcrun` and return
 * a path to the Developer frameworks.
 */
val developerFrameworks: Map<KonanTarget, () -> String> by lazy {
    platformManager.enabled
        .filter { it.family.isAppleFamily }
        .associateWith { target ->
            val configurable = platformManager.platform(target).configurables as AppleConfigurables
            val platform = configurable.platformName().lowercase()
            fun(): String = "${targetPlatform(platform)}/Developer/Library/Frameworks/"
        }
}

/**
 * Gets a path to the developer frameworks location.
 */
fun KonanTarget.getDeveloperFramework(): String = developerFrameworks[this]?.let { it() } ?: error("Not supported target $this")

if (PlatformInfo.isMac()) {
    kotlin {
        val nativeTargets = listOf(
            macosX64(KonanTarget.MACOS_X64.name),
            macosArm64(KonanTarget.MACOS_ARM64.name),
            iosX64(KonanTarget.IOS_X64.name),
            iosArm64(KonanTarget.IOS_ARM64.name),
            iosSimulatorArm64(KonanTarget.IOS_SIMULATOR_ARM64.name)
        )

        nativeTargets.forEach {
            it.compilations.all {
                cinterops {
                    register("XCTest") {
                        compilerOpts("-iframework", konanTarget.getDeveloperFramework())
                    }
                }
            }
        }
        sourceSets.all {
            languageSettings.apply {
                // Oh, yeah! So much experimental, so wow!
                optIn("kotlinx.cinterop.BetaInteropApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }
    }
}

// Due to KT-42056 and KT-48410, it is not possible to set dependencies on dist when opened in the IDEA.
// IDEA sync makes cinterop tasks eagerly resolve dependencies, effectively running the dist-build in configuration time
if (!project.isIdeaActive) {
    targets.forEach {
        val targetName = it.name.capitalize()
        tasks.named<KotlinNativeCompile>("compileKotlin$targetName") {
            dependsOnDist(it)
            dependsOnPlatformLibs(it)
        }

        tasks.named<CInteropProcess>("cinteropXCTest$targetName") {
            dependsOnDist(it)
            dependsOnPlatformLibs(it)
        }
    }
} else {
    tasks.named("prepareKotlinIdeaImport") {
        enabled = false
    }
}

bitcode {
    targets.map { it.withSanitizer() }
        .forEach { targetWithSanitizer ->
            target(targetWithSanitizer) {
                module("xctest") {
                    compilerArgs.set(
                        listOf(
                            "-iframework", target.getDeveloperFramework(),
                            "--std=c++17",
                        )
                    )
                    // Uses headers from the K/N runtime
                    headersDirs.from(project(":kotlin-native:runtime").files("src/main/cpp"))

                    sourceSets { main {} }
                    onlyIf { target.family.isAppleFamily }
                }
            }
        }
}

val xcTestLauncherBitcode by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LLVM_BITCODE))
    }
}

val xcTestArtifactsConfig by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        // Native target-specific
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

dependencies {
    xcTestLauncherBitcode(project)
}

// Each platform has three artifacts: cinterop, regular klib and bc. Add bc file to the klib
targets.forEach { target ->
    val targetName = target.name

    val bitcodeArtifacts = xcTestLauncherBitcode.incoming.artifactView {
        attributes {
            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
        }
    }

    tasks.register("${targetName}XCTestLauncher") {
        description = "Build native launcher for $targetName"
        group = CompileToBitcodeExtension.BUILD_TASK_GROUP
        dependsOn(bitcodeArtifacts.files)
    }

    val runnerKlibProducer = tasks.register<Zip>("${targetName}XCTestRunner") {
        val klibTask = tasks.named<KotlinNativeCompile>("compileKotlin${targetName.capitalize()}")
        dependsOn(klibTask)

        archiveFileName.set("${targetName}XCTest.klib")
        destinationDirectory.set(layout.buildDirectory)

        from(zipTree(klibTask.get().outputFile))
        from(bitcodeArtifacts.files) {
            into("default/targets/$targetName/native")
        }
    }

    artifacts {
        add(xcTestArtifactsConfig.name, runnerKlibProducer) {
            classifier = targetName
        }
        add(xcTestArtifactsConfig.name, tasks.named<CInteropProcess>("cinteropXCTest${targetName.capitalize()}")) {
            classifier = targetName
        }
        add(xcTestArtifactsConfig.name, File(target.getDeveloperFramework())) {
            classifier = "${targetName}Frameworks"
        }
    }
}
