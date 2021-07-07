import org.jetbrains.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
    id("java")
    id("org.jetbrains.dokka") version "1.5.0"
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

// Workaround for https://github.com/bintray/gradle-bintray-plugin/issues/267
//  Manually disable bintray tasks added to the root project
tasks.whenTaskAdded {
    if ("bintray" in name) {
        enabled = false
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
    if (publicationChannels.any { it.isMavenRepository }) {
        finalizedBy(tasks.named("closeAndReleaseSonatypeStagingRepository"))
    }
}
