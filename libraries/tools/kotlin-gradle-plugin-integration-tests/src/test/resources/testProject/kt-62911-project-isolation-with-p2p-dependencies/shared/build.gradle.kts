import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

group = "my.qa.noname"
version = "1.0"

kotlin {
    jvm()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "non-shared"
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
            implementation(project(":hiddenApi"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

    }
}
