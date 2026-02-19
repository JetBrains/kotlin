import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
    application
}

group = "com.example.serialization_app"
version = "1.0"

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("com.example.serialization_lib:serialization_lib:1.0")
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    languageVersion.set(KotlinVersion.KOTLIN_2_0)
}

application {
    mainClass.set("com.example.serialization_app.MainKt")
}
