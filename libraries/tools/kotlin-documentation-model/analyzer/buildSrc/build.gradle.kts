plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    implementation("com.github.jengelman.gradle.plugins:shadow:2.0.4")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.4.0")
    implementation("io.github.gradle-nexus:publish-plugin:1.0.0")
}
