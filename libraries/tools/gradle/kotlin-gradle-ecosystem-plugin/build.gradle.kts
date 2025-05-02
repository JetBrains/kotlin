plugins {
    id("gradle-plugin-common-configuration")
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