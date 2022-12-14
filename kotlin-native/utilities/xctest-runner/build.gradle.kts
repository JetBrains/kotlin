import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.cpp.CppUsage
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
 * By default, K/N includes only SDKs frameworks.
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

fun targetSdk(target: String): String {
    val xcode = Xcode.findCurrent()
    return when (target) {
        "macosx" -> xcode.macosxSdk
        "iphonesimulator" -> xcode.iphonesimulatorSdk
        "iphoneos" -> xcode.iphoneosSdk
        else -> error("Unsupported target $target")
    }
}

val sdkNames = mapOf(
    KonanTarget.MACOS_X64 to "macosx",
    KonanTarget.MACOS_ARM64 to "macosx",
    KonanTarget.IOS_X64 to "iphonesimulator",
    KonanTarget.IOS_SIMULATOR_ARM64 to "iphonesimulator",
    KonanTarget.IOS_ARM64 to "iphoneos"
)

val targets = sdkNames.keys

/*
 * Double laziness: lazily create functions that execute `/usr/bin/xcrun` and return
 * a path to the Developer frameworks.
 */
val developerFrameworks: Map<KonanTarget, () -> String> by lazy {
    platformManager.targetValues
        .filter { it.family.isAppleFamily }
        .associateWith { target ->
            val configurable = platformManager.platform(target).configurables as AppleConfigurables
            val platform = configurable.platformName().toLowerCase()
            fun(): String = "${targetPlatform(platform)}/Developer/Library/Frameworks/"
        }
}

fun KonanTarget.getDeveloperFramework(): String = developerFrameworks[this]?.let { it() } ?: error("Not supported target ${this}")

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
                create("XCTest") {
                    compilerOpts("-iframework", konanTarget.getDeveloperFramework())
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                // Oh, yeah! So much experimental, so wow!
                optIn("kotlinx.cinterop.BetaInteropApi")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
            }
        }
    }
}

targets.forEach {
    val targetName = it.name.capitalize(Locale.getDefault())
    tasks.named<KotlinNativeCompile>("compileKotlin$targetName") {
        dependsOnDist(it)
        dependsOnPlatformLibs(it)
    }

    tasks.named<CInteropProcess>("cinteropXCTest$targetName") {
        dependsOnDist(it)
        dependsOnPlatformLibs(it)
    }
}

bitcode {
    targets.map { it.withSanitizer() }
        .forEach { targetWithSanitizer ->
            target(targetWithSanitizer) {
                module("xctest") {
                    compilerArgs.set(listOf("-iframework", target.getDeveloperFramework()))
                    headersDirs.from(project(":kotlin-native:runtime").files("src/main/cpp"))

                    sourceSets {
                        main {}
                    }
                    onlyIf { target.family.isAppleFamily }
                }
            }
        }
}

val XCTestLauncherBitcode by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(CppUsage.USAGE_ATTRIBUTE, objects.named(CppUsage.LLVM_BITCODE))
    }
}

dependencies {
    XCTestLauncherBitcode(project(project.path))
}

// TODO: each platform has 3 artifacts: cinterop klib, regular klib and bc.
//  Is it possible to merge them into one library like stdlib?
//targets.forEach { target ->
//    val targetName = target.name
//
//    val bitcodeArtifacts = XCTestLauncherBitcode.incoming.artifactView {
//        attributes {
//            attribute(TargetWithSanitizer.TARGET_ATTRIBUTE, target.withSanitizer())
//        }
//    }
//
//    tasks.register("${targetName}XCTestLauncher") {
//        description = "Build native launcher for $targetName"
//        group = CompileToBitcodeExtension.BUILD_TASK_GROUP
//        dependsOn(bitcodeArtifacts.files)
//    }
//
//    tasks.register<Copy>("${targetName}XCTestRunner") {
//        dependsOn(tasks.named<KotlinNativeCompile>("compileKotlin$targetName"))
//        dependsOn("${targetName}XCTestLauncher")
//
//        destinationDir = project.buildDir.resolve("${targetName}XCTest")
//
//        from(project.buildDir.resolve("xctest/${targetName}/"))
//        from(bitcodeArtifacts.files) {
//            into("default/targets/$targetName/native")
//        }
//    }
//}
