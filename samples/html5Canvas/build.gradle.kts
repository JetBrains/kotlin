import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile

plugins {
    kotlin("multiplatform")
}

repositories {
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
}

val hostOs = System.getProperty("os.name")
val isWindows = hostOs.startsWith("Windows")

val packageName = "kotlinx.interop.wasm.dom"
val jsinteropKlibFile = buildDir.resolve("klib").resolve("$packageName-jsinterop.klib")

kotlin {
    wasm32("html5Canvas") {
        binaries {
            executable {
                entryPoint = "sample.html5canvas.main"
            }
        }
    }
    jvm("httpServer")
    sourceSets {
        val html5CanvasMain by getting {
            dependencies {
                implementation(files(jsinteropKlibFile))
            }
        }
        val httpServerMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-netty:1.2.1")
            }
        }
    }
}

val jsinterop by tasks.creating(Exec::class) {
    workingDir = projectDir

    val ext = if (isWindows) ".bat" else ""
    val distributionPath = project.properties["org.jetbrains.kotlin.native.home"] as String?

    if (distributionPath != null) {
        val jsinteropCommand = file(distributionPath).resolve("bin").resolve("jsinterop$ext")

        inputs.property("jsinteropCommand", jsinteropCommand)
        inputs.property("jsinteropPackageName", packageName)
        outputs.file(jsinteropKlibFile)

        commandLine(
            jsinteropCommand,
            "-pkg", packageName,
            "-o", jsinteropKlibFile,
            "-target", "wasm32"
        )
    } else {
        doFirst {
            // Abort build execution if the distribution path isn't specified.
            throw GradleException(
                """
                    |
                    |Kotlin/Native distribution path must be specified to build the JavaScript interop.
                    |Use 'org.jetbrains.kotlin.native.home' project property to specify it.
                """.trimMargin()
            )
        }
    }
}

tasks.withType(AbstractKotlinNativeCompile::class).all {
    dependsOn(jsinterop)
}

val assemble by tasks.getting

// This is to run embedded HTTP server with Ktor:
val runProgram by tasks.creating(JavaExec::class) {
    dependsOn(assemble)

    val httpServer: KotlinJvmTarget by kotlin.targets
    val httpServerMainCompilation = httpServer.compilations["main"]

    main = "sample.html5canvas.httpserver.HttpServer"
    classpath = files(httpServerMainCompilation.output) + httpServerMainCompilation.runtimeDependencyFiles
    args = listOf(projectDir.toString())
}

tasks.withType(KotlinJvmCompile::class).all {
    runProgram.dependsOn(this)
    kotlinOptions.jvmTarget = "1.8"
}
