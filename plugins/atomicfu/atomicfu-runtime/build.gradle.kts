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
        jsMain.dependencies {
            api(kotlin("stdlib-js"))
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
        withType<MavenPublication>().configureEach {
            artifact(emptyJavadocJar)
            val packaging = if (name == "kotlinMultiplatform") "pom" else "klib"
            configureKotlinPomAttributes(
                project,
                explicitDescription = "Runtime library for the Atomicfu compiler plugin",
                packaging = packaging,
            )
        }

        configureSbom(
            target = "Main",
            gradleConfigurations = setOf(),
            publication = named<MavenPublication>("kotlinMultiplatform"),
        )

        configureSbom(
            target = "Js",
            gradleConfigurations = setOf("jsRuntimeClasspath"),
            publication = named<MavenPublication>("js"),
        )
    }
}

configureDefaultPublishing()
