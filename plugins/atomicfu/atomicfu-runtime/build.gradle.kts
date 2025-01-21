import plugins.configureDefaultPublishing
import plugins.configureKotlinPomAttributes

description = "Runtime library for the Atomicfu compiler plugin"

plugins {
    kotlin("js")
    `maven-publish`
    id("nodejs-cache-redirector-configuration")
}

group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
}

kotlin {
    js() {
        browser()
        nodejs()
    }

    sourceSets {
        js().compilations["main"].defaultSourceSet {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}

dependencies {
    implicitDependenciesOnJdkVariantsOfBootstrapStdlib(project)
}

@Suppress("DEPRECATION")
val emptyJavadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            configureKotlinPomAttributes(project, "Runtime library for the Atomicfu compiler plugin", packaging = "klib")
        }
        withType<MavenPublication> {
            artifact(emptyJavadocJar)
        }
    }
}

configureDefaultPublishing()
