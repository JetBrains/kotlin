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

    tasks.create("checkBinaryGetters") {
        doLast {
            println("Wasm binary file: ${wasm32.compilations.getByName("main").getBinary("EXECUTABLE", "RELEASE").name}")
            println("Wasm link task: ${wasm32.compilations.getByName("main").getLinkTask("EXECUTABLE", "RELEASE").name}")

            println("Windows test file: ${mingw64.compilations.getByName("test").getBinary("EXECUTABLE", "DEBUG").name}")
            println("Windows test link task: ${mingw64.compilations.getByName("test").getLinkTask("EXECUTABLE", "DEBUG").name}")

            println("MacOS test file: ${macos64.compilations.getByName("test").getBinary("EXECUTABLE", "DEBUG").name}")
            println("MacOS test link task: ${macos64.compilations.getByName("test").getLinkTask("EXECUTABLE", "DEBUG").name}")

            println("Linux test file: ${linux64.compilations.getByName("test").getBinary("EXECUTABLE", "DEBUG").name}")
            println("Linux test link task: ${linux64.compilations.getByName("test").getLinkTask("EXECUTABLE", "DEBUG").name}")
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
