import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind.EXECUTABLE

plugins {
	id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
	id("maven-publish")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
    maven { setUrl("http://dl.bintray.com/kotlin/kotlinx.html/") }
}

kotlin {
	val jvm6 = jvm("jvm6")
	val jvm8 = jvm("jvm8") {
		compilations["main"].kotlinOptions.jvmTarget = "1.8"
	}
	val nodeJs = js("nodeJs")
	val wasm32 = wasm32()
	val linux64 = linuxX64("linux64")
	val mingw64 = mingwX64("mingw64")
	val macos64 = macosX64("macos64")

    configure(listOf(wasm32, linux64, mingw64, macos64)) {
    	compilations.getByName("main") {
	    	outputKinds.add(EXECUTABLE)
	        entryPoint = "com.example.app.native.main"
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