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
        jsMain {
            dependencies {
                compileOnly(project(":kotlin-stdlib"))
            }
        }
    }
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

configureDefaultPublishing()

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "kotlin-js-plain-objects"
            // FIXME: Remove customized publication in KT-83065
            from(kotlin.js().components.single())
            configureKotlinPomAttributes(project, "Annotations library for the JS Plain Objects compiler plugin", packaging = "klib")
        }
        withType<MavenPublication> {
            artifact(emptyJavadocJar)
        }
    }
}