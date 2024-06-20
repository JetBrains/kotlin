import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0) // todo: remove after KT-61706
    }
}