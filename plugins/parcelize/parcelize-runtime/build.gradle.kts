import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.konan.target.HostManager
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

description = "Runtime library for the Parcelize compiler plugin"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    `maven-publish`
    id("signing-convention")
    id("nodejs-configuration")
    id("binaryen-configuration")
}

val publishedArtifactId = "kotlin-parcelize-runtime"
// Maven Central requires a Javadoc-classified artifact for every non-POM publication.
val emptyJavadocJar = tasks.register("emptyJavadocJar", Jar::class) {
    archiveClassifier = "javadoc"
}

kotlin {
    compilerOptions {
        // TODO(KT-50876) Required for reproducible builds.
        freeCompilerArgs.add("-Xklib-relative-path-base=${layout.buildDirectory.get().asFile},${layout.projectDirectory.asFile},$rootDir")
    }

    targets.configureEach {
        configureSbomForTarget()
        mavenPublication {
            artifactId = artifactId.replace(project.name, publishedArtifactId)
            artifact(emptyJavadocJar)
            configureKotlinPomAttributes(
                project = project,
                explicitDescription = provider { project.description },
                explicitName = provider { "Parcelize Runtime" },
                packaging = when (platformType) {
                    KotlinPlatformType.common, KotlinPlatformType.jvm -> "jar"
                    KotlinPlatformType.js, KotlinPlatformType.native, KotlinPlatformType.wasm -> "klib"
                    KotlinPlatformType.androidJvm -> error("unexpected Android JVM target")
                },
            )
        }
    }

    metadata() // For common sources in IDE

    jvm()

    js {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    if (kotlinBuildProperties.isInIdeaSync.get()) {
        // This is required because of the common source set dependency on a local stdlib.
        // Only these targets are added in the stdlib project during IDEA sync.
        when {
            HostManager.hostIsMac -> @Suppress("DEPRECATION") macosX64("native")
            HostManager.hostIsMingw -> mingwX64("native")
            HostManager.hostIsLinux -> linuxX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }
    } else {
        // Tier 1
        macosArm64()
        iosSimulatorArm64()
        iosArm64()

        // Tier 2
        linuxX64()
        linuxArm64()
        watchosSimulatorArm64()
        watchosArm64()
        tvosSimulatorArm64()
        tvosArm64()

        // Tier 3
        mingwX64()
        watchosDeviceArm64()
        iosX64()
    }

    sourceSets {
        commonMain.dependencies {
            api(kotlinStdlib())
        }
        jvmMain.dependencies {
            compileOnly(commonDependency("com.google.android", "android"))
        }
    }
}

// Keep the historical local JAR name used by the compiler distribution.
tasks.named<Jar>("jvmJar") {
    setupPublicJar("parcelize-runtime")
    archiveAppendix = ""
}

val httpClientVersion = libs.versions.http.client.get()
val jsonVersion = libs.versions.json.get()
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.apache.httpcomponents" && requested.name == "httpclient") {
            useVersion(httpClientVersion)
        }
        if (requested.group == "org.json" && requested.name == "json") {
            useVersion(jsonVersion)
        }
    }
}

configureDefaultPublishing()

// TODO(KT-85034): mavenPublication doesn't work for metadata
publishing {
    publications.configureEach {
        if (this is MavenPublication && name == "kotlinMultiplatform") {
            artifactId = publishedArtifactId
            artifact(emptyJavadocJar)
            project.configureSbomForTarget(kotlin.targets["metadata"], this)
            configureKotlinPomAttributes(
                project = project,
                explicitDescription = provider { project.description },
                explicitName = provider { "Parcelize Runtime" },
            )
        }
    }
}
