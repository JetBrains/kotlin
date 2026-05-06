import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `java-gradle-plugin`
    `embedded-kotlin`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

description = "Generates KGP npm tooling dependency versions."

repositories {
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    gradlePluginPortal()
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation
    jvmToolchain(17)
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.gradlePlugin.gradle.node)
}

gradlePlugin {
    plugins {
        register("KgpNpmToolingHelperPlugin") {
            id = "kotlin-git.gradle-build-conventions.kgp-npm-tooling-helper"
            implementationClass = "org.jetbrains.kotlin.build.kgpnpmtooling.KgpNpmToolingHelperPlugin"
        }
    }
}

project.configurations.named(org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main") {
    resolutionStrategy {
        eachDependency {
            if (this.requested.group == "org.jetbrains.kotlin") useVersion(libs.versions.kotlin.`for`.gradle.plugins.compilation.get())
        }
    }
}

kotlin.compilerOptions.moduleName.value(project.name)
