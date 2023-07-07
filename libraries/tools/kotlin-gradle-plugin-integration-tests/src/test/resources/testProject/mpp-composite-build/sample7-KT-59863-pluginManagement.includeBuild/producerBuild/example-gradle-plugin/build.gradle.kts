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