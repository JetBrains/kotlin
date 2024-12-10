import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl

plugins {
    kotlin("multiplatform") version "2.0"
}

val kotlinVersion: String = "2.0"

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            @OptIn(ExperimentalDistributionDsl::class)
            distribution {
                outputDirectory.set(project.file("js"))
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            }
            kotlin.srcDir("../../benchmarks/shared/src")
        }
        val jsMain by getting {
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("../shared/src/main/kotlin")
            kotlin.srcDir("../src/main/kotlin-js")
        }
    }
}
