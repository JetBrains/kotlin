import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
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
extra["kotlin.native.home"] = konanHome

tasks {
    // Something common again, just turn it off
    compileKotlinMetadata {
        enabled = false
    }
}

// TODO: add check for XCode presence and version

/**
 * By default, K/N includes only SDKs frameworks. It's required to get Library frameworks path
 * where `XCTest.framework` is located.
 */
fun targetPlatform(target: String): String {
    val out = ByteArrayOutputStream()
    val result = project.exec {
        executable = "xcrun"
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

/**
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
        iosX64(KonanTarget.IOS_X64.name),
        iosArm64(KonanTarget.IOS_ARM64.name)
    )

    nativeTargets.forEach {
        it.binaries {
            // TODO: XCTest.framework should be copied to @rpath/Frameworks instead of adding rpath
            val target = this.target.konanTarget
            val frameworkPath = getDeveloperFramework(target)
            val linkerRpath = listOf("-rpath", frameworkPath)
            val frameworkOpt = "-F$frameworkPath"
            framework {
                freeCompilerArgs = listOf("-Xomit-framework-binary")
                linkerOpts(linkerRpath)
                linkerOpts(frameworkOpt)
                binaryOption("bundleId", "XCTestNative")
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
        .forEach {
            target(it) {  // TODO: should be all supported targets
                if (target.family.isAppleFamily) {
                    module("xctest") {
                        // TODO: needs dependency on framework producer task
                        val xctestFramework = kotlin.targets
                            .withType(KotlinNativeTarget::class.java)
                            .filter { it.konanTarget == target }
                            .map { it.binaries.getFramework("DEBUG") }
                            .single()
                            .outputFile

                        // TODO: Needs header XCTest.h from the framework, thus require Xcode to be present during the build
                        compilerArgs.set(
                            listOfNotNull(
                                "-iframework", getDeveloperFramework(target),
                                "-iframework", xctestFramework.parent
                            )
                        )
                        headersDirs.from(files("src/main/cpp"))

                        sourceSets {
                            main {}
                        }
                        onlyIf { target.family.isAppleFamily }
                    }
                }
            }
        }
}

// This produces test bunsdle manually from framework and obj-c launcher
//listOf(KonanTarget.MACOS_X64, KonanTarget.IOS_X64, KonanTarget.IOS_ARM64).forEach { target ->
//    tasks.register("buildTestBundle${target.name}") {
//        dependsOn(":kotlin-native:${target}CrossDist")
//        dependsOn(":kotlin-native:${target}PlatformLibs")
//        dependsOn(tasks.named("${target.name}Binaries"))
//
//        val devFramework = developerFrameworks[target]?.let { it() } ?: error("Not supported target $target")
//
//        val frameworks = kotlin.targets
//                .withType(KotlinNativeTarget::class.java)
//                .filter { nativeTarget -> nativeTarget.konanTarget == target }
//                .map { it.binaries.getFramework("DEBUG") }
//
//        val framework = frameworks.single().outputFile
//        println(framework.absolutePath)
//
//        val targetTriple = when (target) {
//            is KonanTarget.MACOS_X64 -> "x86_64-apple-macos"
//            is KonanTarget.IOS_X64 -> "x86_64-apple-ios-simulator"
//            is KonanTarget.IOS_ARM64 -> "arm64-apple-ios"
//            else -> error("Not configured target")
//        }
//
//        val targetSysroot = sdkNames[target]?.let { sdk -> targetPlatform(sdk) } ?: error("Wrong target $target")
//        val sysroot = File(targetSysroot)
//        val sdkFrameworks = sdkNames[target]
//                ?.let { sdk -> File(targetSdk(sdk)) }
//                ?: error("Not specified SDK for $target")
//
//        val launcher = project.file("objc/Launcher.m")
//        doFirst {
//            project.exec {
//                executable = "clang"
//                args(
//                        listOf(
//                                "-isysroot", sdkFrameworks,
//                                "--target=$targetTriple",
//                                "-lobjc", "-fobjc-arc", "-fPIC",
//                                "-iframework", sdkFrameworks.resolve("System/Library/Frameworks").toString(),
//                                "-iframework", devFramework,
//                                "-iframework", framework.parent,
//                                "-framework", "xctest_runner",
//                                "-framework", "XCTest",
//                                "-Xlinker", "-rpath", devFramework,
//                                "-Xlinker", "-rpath", framework.parent,
//                                "-Xlinker", "-syslibroot", sysroot,
//                                "-Xlinker", "-v",
//                                "-L${framework.absoluteFile.parent}",
//                                "-L${devFramework}",
//                                "-v",
//                                launcher.absolutePath,
//                                "-bundle", "-o", "build/testBundle-${target.name}"
//                        )
//                )
//            }
//        }
//    }
//}


/*
 1. gradle :kotlin-native:utilities:xctest-runner:macos_x64Binaries
 2. gradle :kotlin-native:utilities:xctest-runner:llvmLinkXctestMainMacos_x64
 3.
 */