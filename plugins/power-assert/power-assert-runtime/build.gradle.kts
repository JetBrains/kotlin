import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.konan.target.HostManager
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("signing-convention")
    id("nodejs-cache-redirector-configuration")
    id("binaryen-configuration")
}

description = "Kotlin Power-Assert Runtime"

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier = "javadoc"
}

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xreturn-value-checker=full",
            "-Xallow-kotlin-package",
            // TODO(KT-50876) Required for reproducible builds.
            "-Xklib-relative-path-base=${layout.buildDirectory.get().asFile},${layout.projectDirectory.asFile},$rootDir",
        )
    }

    targets.all {
        configureSbomForTarget()
        mavenPublication {
            // Maven Central requires a Javadoc classified artifact for every non-'pom` publication.
            artifact(emptyJavadocJar)
            configureKotlinPomAttributes(
                project = project,
                explicitDescription = provider { project.description },
                explicitName = provider { project.description },
                // SBOMs are added without a classifier. This means Gradle tries to set packaging to "pom" because there are 2 published
                // artifacts without classifiers. So we need to be explicit about what packaging is used on each platform.
                packaging = when (platformType) {
                    KotlinPlatformType.common, KotlinPlatformType.jvm -> "jar"
                    KotlinPlatformType.js, KotlinPlatformType.native, KotlinPlatformType.wasm -> "klib"
                    // An Android JVM target is redundant and not expected.
                    KotlinPlatformType.androidJvm -> error("unexpected platform type; was a JVM android target added accidentally?")
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
        watchosArm32()
        watchosArm64()
        tvosSimulatorArm64()
        tvosArm64()

        // Tier 3
        androidNativeArm32()
        androidNativeArm64()
        androidNativeX86()
        androidNativeX64()
        mingwX64()
        watchosDeviceArm64()
        @Suppress("DEPRECATION") macosX64()
        @Suppress("DEPRECATION") iosX64()
        @Suppress("DEPRECATION") watchosX64()
        @Suppress("DEPRECATION") tvosX64()
    }

    sourceSets {
        commonMain.dependencies {
            api(kotlinStdlib())
        }
        commonTest.dependencies {
            api(kotlinTest())
        }
        jvmTest.dependencies {
            implementation(kotlinTest("junit"))
        }
    }
}

configureDefaultPublishing()

// TODO(KT-85034): mavenPublication doesn't work for metadata
publishing {
    publications.configureEach {
        if (this is MavenPublication && name == "kotlinMultiplatform") {
            // Maven Central requires a Javadoc classified artifact for every non-'pom` publication.
            artifact(emptyJavadocJar)
            project.configureSbomForTarget(kotlin.targets["metadata"], this)
            configureKotlinPomAttributes(
                project = project,
                explicitDescription = provider { project.description },
                explicitName = provider { project.description },
            )
        }
    }
}
