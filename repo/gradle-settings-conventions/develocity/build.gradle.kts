plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    gradlePluginPortal()
}

kotlin.jvmToolchain(8)

val buildGradlePluginVersion = extra.get("kotlin.build.gradlePlugin.version")
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
    implementation(libs.develocity.gradlePlugin)
    implementation(libs.gradle.customUserData.gradlePlugin)
}
