import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    google { setUrl("https://cache-redirector.jetbrains.com/dl.google.com/dl/android/maven2") }
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

dependencies {
    api(project(":gradle-plugins-common"))

    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion.get()}")
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.downloadTask.gradlePlugin)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")

    constraints {
        api(libs.apache.commons.lang)
    }
}

project.configurations.named(org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main") {
    resolutionStrategy {
        eachDependency {
            if (this.requested.group == "org.jetbrains.kotlin") useVersion(libs.versions.kotlin.`for`.gradle.plugins.compilation.get())
        }
    }
}
