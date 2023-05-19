import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("com.google.code.gson:gson:2.8.9")
    }
}

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion("2.8.9")
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        implementation(project(":kotlin-native-shared"))
    } else {
        implementation(project(":native:kotlin-native-utils"))
    }
}

group = "org.jetbrains.kotlin"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs += listOf(
                "-Xskip-prerelease-check",
                "-Xsuppress-version-warnings",
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
