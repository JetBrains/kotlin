import org.jetbrains.ValidatePublications
import org.jetbrains.configureDokkaVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("java")
}

val dokka_version: String by project

allprojects {
    configureDokkaVersion()

    group = "org.jetbrains.dokka"
    version = dokka_version

    val language_version: String by project
    tasks.withType(KotlinCompile::class).all {
        kotlinOptions {
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xopt-in=kotlin.RequiresOptIn",
                "-Xskip-metadata-version-check",
                "-Xjsr305=strict"
            )
            languageVersion = language_version
            apiVersion = language_version
            jvmTarget = "1.8"
        }
    }

    repositories {
        jcenter()
        mavenCentral()
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
        maven(url = "https://dl.bintray.com/kotlin/kotlin-dev")
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("java")
    }

    // Gradle metadata
    java {
        @Suppress("UnstableApiUsage")
        withSourcesJar()
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// Workaround for https://github.com/bintray/gradle-bintray-plugin/issues/267
//  Manually disable bintray tasks added to the root project
tasks.whenTaskAdded {
    if ("bintray" in name) {
        enabled = false
    }
}

println("Publication version: $dokka_version")
tasks.register<ValidatePublications>("validatePublications")
