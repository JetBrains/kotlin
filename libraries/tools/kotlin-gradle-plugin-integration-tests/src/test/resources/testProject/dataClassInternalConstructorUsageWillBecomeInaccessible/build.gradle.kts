import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":lib"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(18)
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.progressiveMode.set(true)
    compilerOptions.languageVersion.set(KotlinVersion.KOTLIN_2_0)
}