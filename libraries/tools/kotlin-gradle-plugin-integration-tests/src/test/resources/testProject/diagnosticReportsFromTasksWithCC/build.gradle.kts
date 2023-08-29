plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin.jvmToolchain(11)
kotlin.compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
