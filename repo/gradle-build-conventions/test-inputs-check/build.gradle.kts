plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion.get()}")
}

