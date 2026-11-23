import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `kotlin-dsl`
    kotlin("jvm")
}

group = "org.jetbrains.kotlin"

dependencies {
    compileOnly(kotlin("stdlib"))
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreKotlinLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)
}

sourceSets {
    main {
        java.srcDirs("src")
    }
}

gradlePlugin {
    plugins.create("kotlinBuildHelpers") {
        id = "kotlin-build-helpers"
        implementationClass = "KotlinBuildHelpersPlugin"
    }
}
