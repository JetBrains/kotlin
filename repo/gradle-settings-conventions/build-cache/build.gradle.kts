plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
}

kotlin.jvmToolchain(8)

val buildGradlePluginVersion = extra.get("kotlin.build.gradlePlugin.version")
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:$buildGradlePluginVersion")
}
