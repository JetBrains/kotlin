import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

repositories {
    google()
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlin-dev")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

plugins {
    kotlin("multiplatform")
    id("com.android.application") version("3.3.1")
}

val appDir = buildDir.resolve("Polyhedron")
val libsDir = appDir.resolve("libs")

val androidPresets = mapOf(
    "arm32" to ("androidNativeArm32" to "$libsDir/armeabi-v7a"),
    "arm64" to ("androidNativeArm64" to "$libsDir/arm64-v8a")
)

android {
    compileSdkVersion(28)

    defaultConfig {
        applicationId = "com.jetbrains.konan_activity2"
        minSdkVersion(9)
        targetSdkVersion(28)

        ndk {
            abiFilters("armeabi-v7a", "arm64-v8a")
        }
    }

    sourceSets {
        val main by getting {
            setRoot("src/arm32Main")
            jniLibs.srcDir(libsDir)
        }
    }
}

kotlin {
    androidPresets.forEach { (targetName, presetInfo) ->
        val (presetName, _) = presetInfo
        val preset = kotlin.presets[presetName] as KotlinNativeTargetPreset
        targetFromPreset(preset, targetName) {
            binaries {
                executable {
                    entryPoint = "sample.androidnative.main"
                }
            }
            compilations["main"].cinterops {
                val bmpformat by creating
            }
        }
    }

    sourceSets {
        val arm32Main by getting
        val arm64Main by getting
        arm64Main.dependsOn(arm32Main)
    }
}

// Disable generating Kotlin metadata.
tasks.compileKotlinMetadata {
    enabled = false
}

afterEvaluate {
    androidPresets.forEach { (targetName, presetInfo) ->
        val target = kotlin.targets[targetName] as KotlinNativeTarget
        val (_, libDir) = presetInfo

        NativeBuildType.values().forEach {
            val executable = target.binaries.getExecutable(it)
            val linkTask = executable.linkTask
            val binaryFile = executable.outputFile

            linkTask.doLast {
                copy {
                    from(binaryFile)
                    into(libDir)
                    rename { "libpoly.so" }
                }
            }

            tasks.preBuild {
                dependsOn(linkTask)
            }
        }
    }
}
