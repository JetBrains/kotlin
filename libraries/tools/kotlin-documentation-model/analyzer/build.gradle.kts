import org.jetbrains.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("java")
    id("org.jetbrains.dokka") version "1.6.20"
    id("io.github.gradle-nexus.publish-plugin")
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
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjsr305=strict",
                "-Xskip-metadata-version-check",
                // need 1.4 support, otherwise there might be problems with Gradle 6.x (it's bundling Kotlin 1.4)
                "-Xsuppress-version-warnings"
            )
            allWarningsAsErrors = true
            languageVersion = language_version
            apiVersion = language_version
            jvmTarget = "1.8"
        }
    }

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("java")
        plugin("signing")
        plugin("org.jetbrains.dokka")
    }

    // Gradle metadata
    java {
        @Suppress("UnstableApiUsage")
        withSourcesJar()
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks {
        val dokkaOutputDir = "$buildDir/dokka"

        dokkaHtml {
            onlyIf { !isLocalPublication }
            outputDirectory.set(file(dokkaOutputDir))
        }

        register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            dependsOn(dokkaHtml)
            from(dokkaOutputDir)
        }
    }
}

println("Publication version: $dokka_version")
tasks.register<ValidatePublications>("validatePublications")

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

tasks.maybeCreate("dokkaPublish").run {
    if (publicationChannels.any { it.isMavenRepository() }) {
        finalizedBy(tasks.named("closeAndReleaseSonatypeStagingRepository"))
    }
}
