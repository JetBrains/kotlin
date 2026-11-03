import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.external.*
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.CompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetDescriptor.TargetFactory
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.HostManager
import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

description = "Runtime library for the Parcelize compiler plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    `maven-publish`
    id("signing-convention")
    id("nodejs-configuration")
    id("binaryen-configuration")
    id("wasmtime-configuration")
}

val publishedArtifactId = "kotlin-parcelize-runtime"
// Maven Central requires a Javadoc-classified artifact for every non-POM publication.
val emptyJavadocJar = tasks.register("emptyJavadocJar", Jar::class) {
    archiveClassifier = "javadoc"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("jvmAndAndroid") {
                withJvm()
                withCompilations { it.target is AndroidJvmTarget }
            }
            group("nonAndroid") {
                withJvm()
                withJs()
                withWasmJs()
                withWasmWasi()
                group("native") {
                    withNative()
                }
            }
        }
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
        if (this !is KotlinJvmTarget && this is HasConfigurableKotlinCompilerOptions<*>) {
            compilerOptions {
                // TODO(KT-50876) Required for reproducible builds.
                freeCompilerArgs.add("-Xklib-relative-path-base=${layout.buildDirectory.get().asFile},${layout.projectDirectory.asFile},$rootDir")
            }
        }
    }

    metadata() // For common sources in IDE

    jvm()
    androidJvm()
    val buildFeatures = serviceOf<BuildFeatures>()
    js {
        if (!buildFeatures.isolatedProjects.active.get()) {
            browser()
            nodejs()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        if (!buildFeatures.isolatedProjects.active.get()) {
            nodejs()
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        if (!buildFeatures.isolatedProjects.active.get()) {
            wasmtime()
        }
    }

    if (kotlinBuildProperties.isInIdeaSync.get()) {
        // This is required because of the common source set dependency on a local stdlib.
        // Only these targets are added in the stdlib project during IDEA sync.
        // Keep the default target names. The name "native" clashes with the "native" group of the hierarchy template.
        when {
            HostManager.hostIsMac -> @Suppress("DEPRECATION", "DEPRECATION_ERROR") macosX64()
            HostManager.hostIsMingw -> mingwX64()
            HostManager.hostIsLinux -> linuxX64()
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
        iosX64()
        mingwX64()
        watchosDeviceArm64()

        // Deprecated
        @Suppress("DEPRECATION")
        androidNativeArm32()
        @Suppress("DEPRECATION")
        androidNativeArm64()
        @Suppress("DEPRECATION")
        androidNativeX86()
        @Suppress("DEPRECATION")
        androidNativeX64()
    }

    sourceSets {
        commonMain.dependencies {
            api(kotlinStdlib())
        }
        named("jvmAndAndroidMain").dependencies {
            compileOnly(commonDependency("com.google.android", "android"))
        }
    }
}

// Keep the historical local JAR name used by the compiler distribution.
tasks.named<Jar>("jvmJar") {
    setupPublicJar("parcelize-runtime")
    archiveAppendix = ""
}

val commonMetadataJar = tasks.register<Jar>("commonMetadataJar") {
    archiveAppendix = "common-metadata"
    archiveExtension = "klib"
}
kotlin.metadata().compilations.named { it == "commonMain" }.configureEach {
    commonMetadataJar.configure { from(output.allOutputs) }
}
configurations.consumable("commonMainMetadataElements") {
    outgoing.artifact(commonMetadataJar)
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

/**
 * Declares the `android` target. This JVM target compiles the `androidMain` source set into a plain JAR for Android consumers.
 *
 * KGP accepts only one `jvm()` target in a project, and the Android Gradle plugin builds an AAR.
 * Thus, this function uses the external target API, and it does not need the Android Gradle plugin.
 *
 * KGP and the IDE handle the target as a JVM target. Only the variants of the target have the Android attributes.
 * Thus, Gradle selects these variants for Android consumers and the `jvm()` variants for JVM consumers.
 */
@OptIn(ExternalKotlinTargetApi::class)
private fun KotlinMultiplatformExtension.androidJvm(): KotlinTarget {
    val target = createExternalKotlinTarget {
        targetName = "android"
        platformType = KotlinPlatformType.jvm
        targetFactory = TargetFactory(::AndroidJvmTarget)
    }
    val androidMain = sourceSets.maybeCreate("androidMain")
    val main = target.createCompilation {
        compilationName = KotlinCompilation.MAIN_COMPILATION_NAME
        defaultSourceSet = androidMain
        compilationFactory = CompilationFactory(::AndroidJvmCompilation)
    }
    val project = target.project
    val jar = project.tasks.named<Jar>(target.artifactsTaskName) {
        from(main.output.allOutputs)
        archiveAppendix = target.targetName
    }

    val dependencies = project.configurations.getByName(main.runtimeDependencyConfigurationName!!)
    val binaryConfigurations = with(target) {
        listOf(
            apiElementsConfiguration,
            runtimeElementsConfiguration,
            apiElementsPublishedConfiguration,
            runtimeElementsPublishedConfiguration
        )
    }
    binaryConfigurations.forEach { configuration ->
        configuration.outgoing.artifact(jar)
        configuration.extendsFrom(dependencies)
    }
    val sourcesJar = target.sourcesJarTask(main)
    target.sourcesElementsConfiguration.outgoing.artifact(sourcesJar) { classifier = "sources" }
    target.publishSources(sourcesJar)
    val sourcesConfigurations = listOf(target.sourcesElementsConfiguration, target.sourcesElementsPublishedConfiguration)
    (binaryConfigurations + sourcesConfigurations).forEach { configuration ->
        configuration.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
        configuration.attributes.attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            project.objects.named<TargetJvmEnvironment>(TargetJvmEnvironment.ANDROID),
        )
    }
    return target
}

@OptIn(ExternalKotlinTargetApi::class)
private class AndroidJvmTarget(delegate: DecoratedExternalKotlinTarget.Delegate) : DecoratedExternalKotlinTarget(delegate)

@OptIn(ExternalKotlinTargetApi::class)
private class AndroidJvmCompilation(delegate: DecoratedExternalKotlinCompilation.Delegate) : DecoratedExternalKotlinCompilation(delegate)
