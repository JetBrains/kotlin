import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

buildscript {
    dependencies {
        classpath("com.google.code.gson:gson:2.8.9")
    }
}


plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.sam.with.receiver")
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")

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
    implementation(project(":kotlin-native-shared"))
}

group = "org.jetbrains.kotlin"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.apply {
    kotlinOptions {
        freeCompilerArgs += listOf(
                "-Xskip-prerelease-check",
                "-Xsuppress-version-warnings",
                "-opt-in=kotlin.ExperimentalStdlibApi",
                "-opt-in=kotlin.RequiresOptIn"
        )
    }
}