plugins {
    kotlin("multiplatform")
}

group = "org.sample.kt72965"
version = 1.0

repositories {
    maven("<localRepo>")
}

kotlin {
    js {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.sample.kt72965:lib-annotation-site:1.0")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile<*>>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xpartial-linkage-loglevel=warning")
}
