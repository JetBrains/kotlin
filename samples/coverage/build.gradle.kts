import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }

    macosX64("macos") {
        binaries {
            executable(listOf(DEBUG))
        }
        binaries.getTest("DEBUG").apply {
            freeCompilerArgs += listOf("-Xlibrary-to-cover=${compilations["main"].output.classesDirs.singleFile.absolutePath}")
        }
    }
}

tasks.create("createCoverageReport") {
    dependsOn("macosTest")

    description = "Create coverage report"

    doLast {
        val testDebugBinary = kotlin.targets["macos"].let { it as KotlinNativeTarget }.binaries.getExecutable("test", "debug").outputFile
        exec {
            commandLine("llvm-profdata", "merge", "$testDebugBinary.profraw", "-o", "$testDebugBinary.profdata")
        }
        exec {
            commandLine("llvm-cov", "show", "$testDebugBinary", "-instr-profile", "$testDebugBinary.profdata")
        }
    }
}