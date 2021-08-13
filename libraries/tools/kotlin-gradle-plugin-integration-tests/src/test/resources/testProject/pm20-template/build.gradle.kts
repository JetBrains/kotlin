@file:Suppress("unused_variable")

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

plugins {
    kotlin("multiplatform").apply(false)
    kotlin("multiplatform.pm20").apply(false)
}

allprojects {
    group = "com.example"
    version = "1.0"
}

allprojects {
    pluginManager.withPlugin("maven-publish") {
        configure<PublishingExtension> {
            repositories {
                maven("$rootDir/build/repo")
            }
        }
    }
}
