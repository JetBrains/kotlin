import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(false)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    api(project(":gradle-plugins-common"))

    implementation(kotlinBuildHelpers())
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.downloadTask.gradlePlugin)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api")

    constraints {
        api(libs.apache.commons.lang)
    }
}

kotlin.compilerOptions.moduleName.value(project.name)
