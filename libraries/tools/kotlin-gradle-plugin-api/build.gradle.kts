import gradle.GradlePluginVariant
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("gradle-plugin-dependency-configuration")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("gradle-plugin-api-reference")
}

pluginApiReference {
    enableForAllGradlePluginVariants()
    enableKotlinlangDocumentation()

    failOnWarning = true
    moduleName("The Kotlin Gradle plugins API")

    additionalDokkaConfiguration {
        reportUndocumented.set(true)
        includes.from("api-reference-description.md")
    }
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-annotations"))
    commonApi(project(":native:kotlin-native-utils")) { // TODO: consider removing in KT-70247
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-util-klib")
    }
    commonApi(project(":kotlin-tooling-core"))
    commonApi(project(":compiler:build-tools:kotlin-build-tools-api"))

    commonCompileOnly(project(":kotlin-gradle-compiler-types"))

    embedded(project(":kotlin-gradle-compiler-types")) { isTransitive = false }
}

apiValidation {
    nonPublicMarkers += "org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi"
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}

registerKotlinSourceForVersionRange(
    GradlePluginVariant.GRADLE_MIN,
    GradlePluginVariant.GRADLE_88,
)
