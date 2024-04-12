import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.progressiveMode.set(true)
    compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_2_0)
}