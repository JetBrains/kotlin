import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(false)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    implementation(project(":utilities"))
    implementation(kotlinBuildHelpers())
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
}

kotlin.compilerOptions.moduleName.value(project.name)
