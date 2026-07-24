import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

description = "Runtime library for the Atomicfu compiler plugin"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("multiplatform")
    `maven-publish`
    id("nodejs-configuration")
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

val emptyJavadocJar = tasks.register("emptyJavadocJar", Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // FIXME: Remove customized publication in KT-83065
            from(kotlin.js().components.single())
            configureKotlinPomAttributes(project, "Runtime library for the Atomicfu compiler plugin", packaging = "klib")
        }
        withType<MavenPublication> {
            artifact(emptyJavadocJar)
        }
    }
}

configureDefaultPublishing()
