import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = "com.example"
version = "1.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.languageVersion = "<language-version>"
}
