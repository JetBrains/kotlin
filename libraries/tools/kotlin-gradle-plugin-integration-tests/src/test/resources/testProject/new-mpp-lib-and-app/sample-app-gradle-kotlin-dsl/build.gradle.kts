plugins {
	id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
	id("maven-publish")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
	val jvm6 = jvm("jvm6") {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 6)
    }
	val jvm8 = jvm("jvm8") {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
		compilations["main"].kotlinOptions.jvmTarget = "1.8"
	}
	val nodeJs = js("nodeJs") {
        nodejs()
    }
	val linux64 = linuxX64("linux64")

    wasmJs {
    }

    configure(listOf(linux64)) {
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
        jvm6.compilations["main"].defaultSourceSet {
            dependsOn(allJvm)
        }
        jvm8.compilations["main"].defaultSourceSet {
            dependsOn(allJvm)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
            }
        }
        nodeJs.compilations["main"].defaultSourceSet {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
            }
        }
    }
}

tasks.create("resolveRuntimeDependencies", DefaultTask::class.java) {
    doFirst { 
        // KT-26301
        val configName = kotlin.jvm("jvm6").compilations["main"].runtimeDependencyConfigurationName
        configurations[configName].resolve()
    }
}
