import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "com.example"
version = "1.0"

val shouldBeJs = true

kotlin {
    if (shouldBeJs) {
        js("nodeJs") {
            nodejs()
        }
        wasmJs()
    }

    jvm("jvm6") {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 6)
    }
    jvm("jvm8") {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
        compilations["main"].kotlinOptions.jvmTarget = "1.8"
    }


    linuxX64("linux64") {
        binaries.executable("main", listOf(DEBUG)) {
            entryPoint = "com.example.app.native.main"
        }

        binaries.all {
            // Check that linker options are correctly passed to the compiler.
            linkerOpts = mutableListOf("-L.")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.example:sample-lib:1.0")
            }
        }
        val allJvm by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
            }
        }
        val jvm6Main by getting {
            dependsOn(allJvm)
        }
        val jvm8Main by getting {
            dependsOn(allJvm)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
            }
        }
    }
}

tasks.register("resolveRuntimeDependencies") {
    doFirst {
        // KT-26301
        val configName = kotlin.jvm("jvm6").compilations["main"].runtimeDependencyConfigurationName
        configurations[configName].resolve()
    }
}

publishing {
    repositories {
        maven {
            name = "LocalRepo"
            url = uri("<localRepo>")
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    /** Add a changing input, to enforce re-running KotlinCompile tasks in specific tests, without needing to re-run _all_ tasks. */
    val kotlinCompileCacheBuster = 0
    inputs.property("kotlinCompileCacheBuster", kotlinCompileCacheBuster)

    val kotlinCompileLogLevel = LogLevel.LIFECYCLE
    inputs.property("kotlinCompileLogLevel", kotlinCompileLogLevel)
    logging.captureStandardOutput(kotlinCompileLogLevel)
}
