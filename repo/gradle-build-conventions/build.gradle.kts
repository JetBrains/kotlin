plugins {
    id("org.jetbrains.kotlin.jvm") apply false
}

buildscript {
    dependencies {
        classpath(libs.jackson.module.kotlin)
        classpath(libs.jackson.dataformat.yaml)
    }
}
