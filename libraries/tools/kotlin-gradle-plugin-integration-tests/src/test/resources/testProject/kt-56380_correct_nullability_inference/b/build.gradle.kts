import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":a"))
            }
        }
    }
}

tasks.withType(KotlinCompile::class.java).configureEach {
    kotlinOptions {
        allWarningsAsErrors = true
    }
}