import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.*
import java.io.ByteArrayOutputStream

description = "XCTest wrapper of Native kotlin.test"

plugins {
    kotlin("multiplatform")
}

// region XCTest framework location

/**
 * Value source for the Xcode developer framework path location.
 */
abstract class DevFrameworkPathValueSource : ValueSource<String, DevFrameworkPathValueSource.Parameters> {
    @get:Inject
    abstract val execOperations: ExecOperations

    interface Parameters : ValueSourceParameters {
        val konanTarget: Property<KonanTarget>
    }

    override fun obtain(): String {
        val devFrameworkPath = parameters.konanTarget.get().developerFrameworkPath()

        check(File(devFrameworkPath).exists()) {
            "Developer frameworks path wasn't found at $devFrameworkPath. Check configuration and Xcode installation"
        }

        return parameters.konanTarget.get().developerFrameworkPath()
    }

    /**
     * Path to the target SDK platform.
     *
     * By default, K/N includes only SDK frameworks as platform libs.
     * It's required to get the Library frameworks path where the `XCTest.framework` is located.
     * Consider making XCTest a platform lib with KT-61709.
     */
    private fun targetPlatform(target: String): String {
        val out = ByteArrayOutputStream()
        val result = execOperations.exec {
            executable = "/usr/bin/xcrun"
            args = listOf("--sdk", target, "--show-sdk-platform-path")
            standardOutput = out
        }
        check(result.exitValue == 0) {
            "xcrun failed with ${result.exitValue}. See the output: $out"
        }

        return out.toString().trim()
    }

    /**
     * Returns a path to the Xcode developer frameworks location based on the specified [KonanTarget].
     */
    private fun KonanTarget.developerFrameworkPath(): String {
        val platform = when (this) {
            KonanTarget.MACOS_ARM64, KonanTarget.MACOS_X64 -> "macosx"
            KonanTarget.IOS_SIMULATOR_ARM64, KonanTarget.IOS_X64 -> "iphonesimulator"
            KonanTarget.IOS_ARM64 -> "iphoneos"
            else -> error("Target $this is not supported here")
        }

        return targetPlatform(platform) + "/Developer/Library/Frameworks/"
    }
}

/**
 * Registers a task to copy the XCTest framework to the build directory for the specified [KonanTarget].
 *
 * @param target The [KonanTarget] for which the copy framework task should be registered.
 * @return The [TaskProvider] representing the registered copy framework task.
 */
fun registerCopyFrameworkTask(target: KonanTarget): TaskProvider<Sync> =
    tasks.register<Sync>("${target}FrameworkCopy") {
        into(layout.buildDirectory.dir("$target/Frameworks"))
        from(
            providers.of(DevFrameworkPathValueSource::class) {
                parameters {
                    konanTarget = target
                }
            }
        ) {
            include("XCTest.framework/**")
        }
    }

// endregion

// region Kotlin Multiplatform build configuration

val nativeTargets = mutableListOf<KotlinNativeTarget>()

kotlin {
    with(nativeTargets) {
        add(macosX64())
        add(macosArm64())
        add(iosX64())
        add(iosArm64())
        add(iosSimulatorArm64())

        forEach {
            val copyTask = registerCopyFrameworkTask(it.konanTarget)
            it.compilations.all {
                cinterops {
                    register("XCTest") {
                        compilerOpts("-iframework", copyTask.map { it.destinationDir }.get().absolutePath)
                        // cinterop task should depend on the framework copy task
                        tasks.named(interopProcessingTaskName).configure {
                            dependsOn(copyTask)
                        }
                    }
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

// endregion

// region Artifact collection for consumers

val kotlinTestNativeXCTest by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
    }
}

nativeTargets.forEach { target ->
    val targetName = target.konanTarget.name
    val mainCompilation = target.compilations.getByName("main")
    val outputKlibTask = mainCompilation.compileTaskProvider

    @Suppress("UNCHECKED_CAST")
    val cinteropKlibTask = tasks.named(
        mainCompilation.cinterops
            .getByName("XCTest")
            .interopProcessingTaskName
    ) as? TaskProvider<CInteropProcess> ?: error("Unable to get CInteropProcess task provider")

    artifacts {
        add(kotlinTestNativeXCTest.name, outputKlibTask.flatMap { it.outputFile }) {
            classifier = targetName
            builtBy(outputKlibTask)
        }
        add(kotlinTestNativeXCTest.name, cinteropKlibTask.flatMap { it.outputFileProvider }) {
            classifier = targetName
            builtBy(cinteropKlibTask)
        }
    }
}

// endregion