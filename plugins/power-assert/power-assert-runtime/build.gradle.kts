import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    `maven-publish`
    id("signing-convention")
    id("nodejs-configuration")
    id("binaryen-configuration")
}

description = "Kotlin Power-Assert Runtime"

val emptyJavadocJar = tasks.register("emptyJavadocJar", Jar::class) {
    archiveClassifier = "javadoc"
}

kotlin {
    explicitApi()

    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xreturn-value-checker=full",
            "-Xallow-kotlin-package",
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
        if (this !is KotlinJvmTarget && this is HasConfigurableKotlinCompilerOptions<*>) {
            compilerOptions {
                // TODO(KT-50876) Required for reproducible builds.
                freeCompilerArgs.add("-Xklib-relative-path-base=${layout.buildDirectory.get().asFile},${layout.projectDirectory.asFile},$rootDir",)
            }
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
            HostManager.hostIsMac -> @Suppress("DEPRECATION", "DEPRECATION_ERROR") macosX64("native")
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
        @Suppress("DEPRECATION") androidNativeArm32()
        @Suppress("DEPRECATION") androidNativeArm64()
        @Suppress("DEPRECATION") androidNativeX86()
        @Suppress("DEPRECATION") androidNativeX64()
        mingwX64()
        watchosDeviceArm64()
        @Suppress("DEPRECATION", "DEPRECATION_ERROR") macosX64()
        @Suppress("DEPRECATION", "DEPRECATION_ERROR") iosX64()
        @Suppress("DEPRECATION", "DEPRECATION_ERROR") watchosX64()
        @Suppress("DEPRECATION", "DEPRECATION_ERROR") tvosX64()
        @Suppress("DEPRECATION") linuxArm32Hfp()
    }

    sourceSets {
        commonMain.dependencies {
            api(kotlinStdlib())
        }
        commonTest.dependencies {
            api(kotlinTest())
        }
        jvmTest.dependencies {
            implementation(kotlinTest("junit5"))
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
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
