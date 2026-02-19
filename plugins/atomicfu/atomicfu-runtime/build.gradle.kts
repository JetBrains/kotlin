import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

description = "Runtime library for the Atomicfu compiler plugin"

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("nodejs-cache-redirector-configuration")
}

group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
}

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

dependencies {
    implicitDependenciesOnJdkVariantsOfBootstrapStdlib(project)
}

val emptyJavadocJar by tasks.registering(Jar::class) {
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
