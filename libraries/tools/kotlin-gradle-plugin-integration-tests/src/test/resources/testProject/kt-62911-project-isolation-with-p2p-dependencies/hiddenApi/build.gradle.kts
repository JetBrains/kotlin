import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

kotlin {
    jvm()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    linuxX64 {
        binaries.staticLib {
            baseName = "shared"
        }
    }
    linuxArm64()


    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")

        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

    }
}

publishing {
    publications.configureEach {
        if (name == "kotlinMultiplatform") {
            this as MavenPublication
            groupId = "my-custom-group"
            artifactId = "my-custom-id"
        }
    }
}