import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    @Suppress("Deprecation")
    coreLibrariesVersion = embeddedKotlinVersion
}

dependencies {
    implementation(kotlinBuildHelpers())
    implementation(project(":utilities"))
    implementation(project(":java-flight-recorder"))
}
