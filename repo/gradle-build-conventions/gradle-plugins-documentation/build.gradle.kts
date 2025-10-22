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

    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation(libs.dokka.gradlePlugin)
    implementation(libs.downloadTask.gradlePlugin)

    constraints {
        api(libs.apache.commons.lang)
    }
}

/**
 * Security Version Override for Woodstox XML Parser
 *
 * Forces a consistent, secure version of the Woodstox XML parsing library
 * across all project configurations to mitigate multiple known vulnerabilities.
 *
 * Affected Library:
 * └── com.fasterxml.woodstox:woodstox-core:* → 6.4.0
 *
 * Mitigated Vulnerabilities:
 * - CVE-2022-40156: XML External Entity (XXE) vulnerability
 * - CVE-2022-40155: Information disclosure risk
 * - CVE-2022-40154: Potential code execution vulnerability
 * - CVE-2022-40153: Parsing security bypass
 * - CVE-2022-40152: XML processing vulnerability
 */
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.fasterxml.woodstox" && requested.name == "woodstox-core") {
            useVersion("6.4.0")
            because("CVE-2022-40156, CVE-2022-40155, CVE-2022-40154, CVE-2022-40153, CVE-2022-40152")
        }

        if (requested.group.startsWith("com.fasterxml.jackson")) {
            useVersion("2.16.0")
            because("CVE-2025-49128, CVE-2025-52999")
        }
    }
}
