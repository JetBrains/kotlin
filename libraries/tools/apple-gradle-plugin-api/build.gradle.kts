import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val intellijVersion = rootProject.extra["versions.intellijSdk"] as String

group = "org.jetbrains.gradle.apple"
version = "$intellijVersion-0.1"

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