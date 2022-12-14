import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Xcode
import java.io.ByteArrayOutputStream

plugins {
    id("kotlin.native.build-tools-conventions")
    kotlin("multiplatform")
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
        "xcrun ended unsuccessfully. See output: $out"
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

fun developerFrameworks(): Map<KonanTarget, String> =
        sdkNames.map { it.key to "${targetPlatform(it.value)}/Developer/Library/Frameworks/" }
                .toMap()

kotlin {
    val nativeTargets = listOf(
            macosX64(KonanTarget.MACOS_X64.name),
            iosX64(KonanTarget.IOS_X64.name),
            iosArm64(KonanTarget.IOS_ARM64.name)
    )
    val devFrameworks = developerFrameworks()

    nativeTargets.forEach {
//        it.compilations.all {
//            cinterops {
//                register("xctest") {
//                    defFile =
//                }
//            }
//        }
        it.binaries {
            // TODO: XCTest.framework should be copied to @rpath/Frameworks instead of adding rpath
            val target = this.target.konanTarget
            val frameworkPath = devFrameworks[target] ?: error("Not supported target $target")
            val linkerRpath = listOf("-rpath", frameworkPath)
            val frameworkOpt = "-F$frameworkPath"
            framework {
                freeCompilerArgs = listOf("-tr")
                linkerOpts(linkerRpath)
                linkerOpts(frameworkOpt)
                binaryOption("bundleId", "XCTestNative") // ??
            }
        }
    }
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
            }
        }
    }
}

listOf(KonanTarget.MACOS_X64, KonanTarget.IOS_X64, KonanTarget.IOS_ARM64).forEach {
    tasks.register("buildTestBundle${it.name}") {
        dependsOn(":kotlin-native:${it}CrossDist")
        dependsOn(":kotlin-native:${it}PlatformLibs")
        dependsOn(tasks.named("${it.name}Binaries"))

        val developerFrameworks = developerFrameworks()[it]

        val frameworks = kotlin.targets
                .withType(KotlinNativeTarget::class.java)
                .filter { nativeTarget -> nativeTarget.konanTarget == it }
                .map { it.binaries.getFramework("DEBUG") }

        val framework = frameworks.single().outputFile
        println(framework.absolutePath)

        val targetTriple = when (it) {
            is KonanTarget.MACOS_X64 -> "x86_64-apple-macos"
            is KonanTarget.IOS_X64 -> "x86_64-apple-ios-simulator"
            is KonanTarget.IOS_ARM64 -> "arm64-apple-ios"
            else -> error("Not configured target")
        }

        val targetSysroot = sdkNames[it]?.let { sdk -> targetPlatform(sdk) } ?: error("Wrong target $it")
        val sysroot = File(targetSysroot)
        val sdkFrameworks = sdkNames[it]
                ?.let { sdk -> File(targetSdk(sdk)) }
                ?: error("Not specified SDK for $it")

        val launcher = project.file("objc/Launcher.m")
        doFirst {
            project.exec {
                executable = "clang"
                args(listOf(
                        "-isysroot", sdkFrameworks,
                        "--target=$targetTriple",
                        "-lobjc", "-fobjc-arc", "-fPIC",
                        "-iframework", sdkFrameworks.resolve("System/Library/Frameworks").toString(),
                        "-iframework", developerFrameworks,
                        "-iframework", framework.parent,
                        "-framework", "xctest_runner",
                        "-framework", "XCTest",
                        "-Xlinker", "-rpath", developerFrameworks,
                        "-Xlinker", "-rpath", framework.parent,
                        "-Xlinker", "-syslibroot", sysroot,
                        "-Xlinker", "-v",
                        "-L${framework.absoluteFile.parent}",
                        "-L${developerFrameworks}",
                        "-v",
                        launcher.absolutePath,
                        "-bundle", "-o", "build/testBundle-${it.name}"
                ))
            }
        }
    }
}