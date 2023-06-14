import org.jetbrains.kotlin.PlatformInfo
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.platformManager
import java.io.ByteArrayOutputStream

plugins {
    id("kotlin.native.build-tools-conventions")
    kotlin("multiplatform")
    id("compile-to-bitcode")
}

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
    KonanTarget.IOS_X64 to "iphonesimulator",
    KonanTarget.IOS_ARM64 to "iphoneos"
)

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

fun getDeveloperFramework(target: KonanTarget): String = developerFrameworks[target]?.let { it() } ?: error("Not supported target $target")

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
                    compilerOpts("-iframework", getDeveloperFramework(konanTarget))
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                // Oh, yeah! So much experimental, so wow!
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
                optIn("kotlin.experimental.ExperimentalNativeApi")
                optIn("kotlinx.cinterop.BetaInteropApi")
            }
        }
    }
}

bitcode {
    sdkNames.keys
        .map { it.withSanitizer() }
        .forEach { targetWithSanitizer ->
            target(targetWithSanitizer) {
                module("xctest") {
                    compilerArgs.set(
                        listOf("-iframework", getDeveloperFramework(target))
                    )
                    headersDirs.from(project(":kotlin-native:runtime").files("src/main/cpp"))

                    sourceSets {
                        main {}
                    }
                    onlyIf { target.family.isAppleFamily }
                }
            }
        }
}

/*
0. gradle :kotlin-native:dist
1. gradle :kotlin-native:utilities:xctest-runner:macos_x64Binaries
2. gradle :kotlin-native:utilities:xctest-runner:llvmLinkXctestMainMacos_x64
3. Build the test with
 ~/ws/kotlin/kotlin-native/dist/bin/kotlinc-native -produce test_bundle -tr -Xverbose-phases=Linker
   -linker-option "-F/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/Library/Frameworks/"
   -Xbinary=runtimeAssertionsMode=panic
   -l ./build/classes/kotlin/macos_x64/main/klib/xctest-runner.klib
   -nl ./build/bitcode/main/macos_x64/xctest.bc
   ./src/commonTest/kotlin/Test.kt
 4. xcrun xctest test_bundle.xctest
 */