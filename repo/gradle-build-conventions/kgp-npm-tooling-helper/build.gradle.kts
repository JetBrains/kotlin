import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `java-gradle-plugin`
    `embedded-kotlin`
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

description = "Generates KGP npm tooling dependency versions."

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
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

kotlin.compilerOptions.moduleName.value(project.name)
