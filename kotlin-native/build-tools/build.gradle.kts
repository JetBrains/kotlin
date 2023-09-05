/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

buildscript {
    val rootBuildDirectory by extra(project.file("../.."))
    apply(from = rootBuildDirectory.resolve("kotlin-native/gradle/loadRootProperties.gradle"))

    dependencies {
        classpath("com.google.code.gson:gson:2.8.9")
    }
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

plugins {
    groovy
    kotlin("jvm")
    `kotlin-dsl`
}

dependencies {
    api(gradleApi())

    api("org.jetbrains.kotlin:kotlin-stdlib:${project.bootstrapKotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${project.bootstrapKotlinVersion}") { isTransitive = false }
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")

    // To build Konan Gradle plugin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")

    val versionProperties = Properties()
    project.rootProject.projectDir.resolve("../../gradle/versions.properties").inputStream().use { propInput ->
        versionProperties.load(propInput)
    }
    implementation("com.google.code.gson:gson:2.8.9")
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "com.google.code.gson" && requested.name == "gson") {
                useVersion(versionProperties["versions.gson"] as String)
                because("Force using same gson version because of https://github.com/google/gson/pull/1991")
            }
        }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    val metadataVersion = "0.0.1-dev-10"
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-klib:$metadataVersion")

    implementation("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
    implementation(project(":kotlin-native-executors"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val compileKotlin: KotlinCompile by tasks
val compileGroovy: GroovyCompile by tasks

compileKotlin.apply {
    compilerOptions {
        optIn.add("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.addAll(
            listOf(
                "-Xskip-prerelease-check",
                "-Xsuppress-version-warnings",
                "-Xallow-unstable-dependencies"
            )
        )
    }
}

// Add Kotlin classes to a classpath for the Groovy compiler
compileGroovy.apply {
    classpath += project.files(compileKotlin.destinationDirectory)
    dependsOn(compileKotlin)
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
            kotlin.srcDir("../../kotlin-native/tools/kotlin-native-gradle-plugin/src/main/kotlin")
        }
    }
}

gradlePlugin {
    plugins {
        create("compileToBitcode") {
            id = "compile-to-bitcode"
            implementationClass = "org.jetbrains.kotlin.bitcode.CompileToBitcodePlugin"
        }
        create("runtimeTesting") {
            id = "runtime-testing"
            implementationClass = "org.jetbrains.kotlin.testing.native.RuntimeTestingPlugin"
        }
        create("compilationDatabase") {
            id = "compilation-database"
            implementationClass = "org.jetbrains.kotlin.cpp.CompilationDatabasePlugin"
        }
        create("konanPlugin") {
            id = "konan"
            implementationClass = "org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin"
        }
        create("native-interop-plugin") {
            id = "native-interop-plugin"
            implementationClass = "org.jetbrains.kotlin.NativeInteropPlugin"
        }
        create("native") {
            id = "native"
            implementationClass = "org.jetbrains.kotlin.tools.NativePlugin"
        }
    }
}
