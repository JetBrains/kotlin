import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

kotlin {
    // Kotlin/Native hosts:
    listOf(
            linuxX64(),
            macosArm64(),
            mingwX64(),
    ).forEach {
        it.binaries.executable("benchmarksAnalyzer", listOf(NativeBuildType.RELEASE))
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
            }
            kotlin.srcDir("../../tools/benchmarks/shared/src/main/kotlin")
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}