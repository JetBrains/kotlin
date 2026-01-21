plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
}

kotlin.jvmToolchain(17)

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    }
}
