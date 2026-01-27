import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

val benchmarksAnalyzerExecutable by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("executable"))
    }
}

kotlin {
    // Kotlin/Native hosts:
    listOf(
            linuxX64(),
            macosArm64(),
            mingwX64(),
    ).forEach {
        it.binaries.executable("benchmarksAnalyzer", listOf(NativeBuildType.RELEASE)) {
            benchmarksAnalyzerExecutable.outgoing.variants {
                create(target.name) {
                    artifact(linkTaskProvider.map { it.outputFile.get() })
                    attributes {
                        attribute(KotlinNativeTarget.konanTargetAttribute, target.name)
                    }
                }
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
            }
            kotlin.srcDir("../reports/src/main/kotlin")
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}