import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("exampleGradlePlugin") {
            id = "org.jetbrains.example.gradle.plugin"
            implementationClass = "ExampleGradlePlugin"
        }
    }
}

kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0) // this build produces a gradle plugin that should be consumable by old Gradle versions
    }
}