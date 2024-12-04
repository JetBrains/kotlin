import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    if (properties.containsKey("p2.enableAdditionalTarget")) {
        mingwX64()
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        if (!HostManager().isEnabled(konanTarget)) {
            error("Expected all targets to be supported. $konanTarget is disabled on this host!")
        }
    }

    sourceSets.commonMain.get().dependencies {
        when (project.properties["dependencyMode"]?.toString()) {
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
                implementation("kotlin-multiplatform-projects:p1:1.0.0-SNAPSHOT")
            }
        }
    }
}
