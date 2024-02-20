import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
//    `java-library`
}

kotlin {
    val nativeTargets = listOf(
        linuxX64(),
        // 1) no machine currently available 2) CLI library does not support
//        linuxArm64(),
//        macosX64(),
//        macosArm64(),
    )

    jvm {
        withJava()
        // TODO: this is forbidden by repo settings, but also necessary for jcstress (?)
//        jvmToolchain(8)
    }

    val hostOs = System.getProperty("os.name")
    val affinitySupported = hostOs == "Linux"
    nativeTargets.forEach { target ->
        target.apply {
            compilations.getByName("main") {
                cinterops {
                    val barrier by creating {
                        defFile(project.file("src/nativeInterop/barrier.def"))
                        headers(project.file("src/nativeInterop/barrier.h"))
                    }
                    if (affinitySupported) {
                        val affinity by creating {
                            defFile(project.file("src/nativeInterop/kaffinity.def"))
                            compilerOpts.add("-D_GNU_SOURCE")
                        }
                    }
                }
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":kotlin-stdlib"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        jvmMain {
            dependencies {
                implementation(kotlin("reflect"))
            }
        }

        macosMain {
            kotlin.srcDirs("src/macosMain/kotlin")
        }

        linuxMain {
            kotlin.srcDirs("src/linuxMain/kotlin")
        }
    }
}

//val jcsDir: File get() = File(System.getenv("JCS_DIR") ?: error("JCS_DIR envvar is not set"))
//
//tasks.register<Copy>("copyLibToJCStress") {
//    dependsOn("jvmJar")
//    from(layout.buildDirectory.file("libs/core-jvm-$version.jar"))
//    rename { "litmusktJvm-1.0.jar" }
//    into(jcsDir.resolve("libs/komem/litmus/litmusktJvm/1.0/"))
//}
