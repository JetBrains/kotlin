description = "Runtime library for the JSO compiler plugin"

plugins {
    kotlin("js")
    `maven-publish`
}

group = "org.jetbrains.kotlin"

kotlin {
    js {
        browser()
        nodejs()
        compilations["main"].defaultSourceSet {
            dependencies {
                compileOnly(kotlin("stdlib-js"))
            }
        }
    }
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}