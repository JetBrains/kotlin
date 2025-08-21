import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
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

kotlin.jvmToolchain(8)

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
    compilerOptions.languageVersion.set(<language-version>)
}
