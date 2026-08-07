import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization")
}

description = "Generates KGP npm tooling dependency versions."

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors = true
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

listOf(
    org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
).forEach { confName ->
    project.configurations.named(confName) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(embeddedKotlinVersion)
            }
        }
    }
}
