import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val cidrVersion = rootProject.extra["versions.cidrPlatform"] as String

group = "org.jetbrains.gradle.apple"
version = "$cidrVersion-0.1"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(gradleApi())
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.languageVersion = "1.3"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.freeCompilerArgs += listOf("-Xjvm-default=enable")
    }
}