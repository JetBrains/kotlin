import kotlinx.validation.KotlinApiBuildTask

plugins {
    id("gradle-plugin-common-configuration")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

gradlePlugin {
    plugins {
        create("kotlin-ecosystem-plugin") {
            id = "org.jetbrains.kotlin.ecosystem"
            displayName = "Kotlin Ecosystem plugin"
            description = "Gradle settings plugin providing project wide Kotlin configuration"
            implementationClass = "org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemPlugin"
        }
    }
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
}

tasks.named<KotlinApiBuildTask>("apiBuild") {
    inputJar.value(tasks.named<Jar>("jar").flatMap { it.archiveFile })
}

extra["oldCompilerForGradleCompatibility"] = true
