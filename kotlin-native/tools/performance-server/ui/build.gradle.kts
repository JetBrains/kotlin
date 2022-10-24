import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

buildscript {
    val rootBuildDirectory by extra(file("../../.."))

    java.util.Properties().also {
        it.load(java.io.FileReader(project.file("$rootBuildDirectory/../gradle.properties")))
    }.forEach { k, v ->
        val key = k as String
        val value = project.findProperty(key) ?: v
        extra[key] = value
    }

    extra["withoutEmbedabble"] = true
    project.kotlinInit(findProperty("cacheRedirectorEnabled")?.toString()?.toBoolean() ?: false)
    val bootstrapKotlinRepo: String? by extra(project.bootstrapKotlinRepo)
    val bootstrapKotlinVersion: String by extra(project.bootstrapKotlinVersion)
    val kotlinVersion: String by extra(bootstrapKotlinVersion)

    apply(from = "$rootBuildDirectory/gradle/loadRootProperties.gradle")
    apply(from = "$rootBuildDirectory/gradle/kotlinGradlePlugin.gradle")
}

plugins {
    kotlin("multiplatform")
}

val kotlinVersion: String by extra(bootstrapKotlinVersion)

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            distribution {
                directory = project.file("js")
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
            }
            kotlin.srcDir("../../benchmarks/shared/src")
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
            }
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("../shared/src/main/kotlin")
            kotlin.srcDir("../src/main/kotlin-js")
        }
    }
}
