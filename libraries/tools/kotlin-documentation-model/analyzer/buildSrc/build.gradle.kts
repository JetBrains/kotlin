plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.jengelman.gradle.plugins:shadow:2.0.4")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.9.0")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
}
