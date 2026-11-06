import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
}

val dependencyMode = providers.gradleProperty("dependencyMode")
val enableAdditionalTarget = providers.gradleProperty("p2.enableAdditionalTarget")

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    linuxX64()
    linuxArm64()

    if (enableAdditionalTarget.getOrNull() != null) {
        mingwX64()
    }

    val host = nativeHost
    targets.withType<KotlinNativeTarget>().configureEach {
        if (!host.canLinkBinaries(konanTarget.name)) {
            error("Expected all targets to be supported. $konanTarget is disabled on this host!")
        }
    }

    sourceSets.commonMain.get().dependencies {
        when (dependencyMode.getOrNull()?.toString()) {
            null -> {
                logger.warn("dependencyMode = null -> Using 'project'")
                implementation(project(":p1"))
            }

            "project" -> {
                logger.quiet("dependencyMode = 'project'")
                implementation(project(":p1"))
            }

            "repository" -> {
                logger.quiet("dependencyMode = 'repository'")
                implementation("kotlin-multiplatform-projects:p1:1.0.0")
            }
        }
    }
}
