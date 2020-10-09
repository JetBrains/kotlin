import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "0.1.0-build63"
    application
}

group = "me.user"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":common"))
    implementation(compose.desktop.all)
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MainKt"
}