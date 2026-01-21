plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    google { setUrl("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2") }
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(17)
}

/**
 * Don't add new dependencies here
 */
dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion.get()}")
    api(libs.jetbrains.ideaExt.gradlePlugin)
}
