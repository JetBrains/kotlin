import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    jvmToolchain(17)
}

/**
 * Don't add new dependencies here
 */
dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation(kotlinBuildHelpers())
    api(libs.jetbrains.ideaExt.gradlePlugin)
}

kotlin.compilerOptions.moduleName.value(project.name)
