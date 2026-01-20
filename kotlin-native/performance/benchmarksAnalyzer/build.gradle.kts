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
            }
            kotlin.srcDir("../../tools/benchmarks/shared/src/main/kotlin")
            kotlin.srcDir("../../endorsedLibraries/kotlinx.cli/src/main/kotlin")
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        nativeMain {
            kotlin.srcDir("../../endorsedLibraries/kotlinx.cli/src/main/kotlin-native")
        }
    }
}