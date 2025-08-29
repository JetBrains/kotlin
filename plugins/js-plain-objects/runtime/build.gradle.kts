import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

description = "Runtime library for the JS Plain Objects compiler plugin"

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("nodejs-cache-redirector-configuration")
}

group = "org.jetbrains.kotlin"

kotlin {
    js {
        browser()
        nodejs()
    }

    sourceSets {
        jsMain.dependencies {
            api(kotlin("stdlib-js"))
        }
    }
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

configureDefaultPublishing()

val defaultArtifactId = "kotlin-js-plain-objects"
val extraDescription = "Annotations library for the JS Plain Objects compiler plugin"

publishing {
    publications {
        val mainPublication = named<MavenPublication>("kotlinMultiplatform") {
            artifact(emptyJavadocJar)
            artifactId = defaultArtifactId
            configureKotlinPomAttributes(
                project,
                explicitDescription = extraDescription,
                packaging = "pom",
            )
        }

        val jsPublication = named<MavenPublication>("js") {
            artifact(emptyJavadocJar)
            artifactId = "$defaultArtifactId-js"
            configureKotlinPomAttributes(
                project,
                explicitDescription = extraDescription,
                packaging = "klib",
            )
        }

        configureSbom(
            target = "Main",
            gradleConfigurations = setOf(),
            publication = mainPublication,
        )

        configureSbom(
            target = "Js",
            gradleConfigurations = setOf("jsRuntimeClasspath"),
            publication = jsPublication,
        )
    }
}