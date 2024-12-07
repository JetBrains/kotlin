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
        classpath(libs.gson)
    }
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()
}

plugins {
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

    implementation(libs.gson)

    implementation("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val compileKotlin: KotlinCompile by tasks

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

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
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
        create("native-interop-plugin") {
            id = "native-interop-plugin"
            implementationClass = "org.jetbrains.kotlin.interop.NativeInteropPlugin"
        }
        create("native") {
            id = "native"
            implementationClass = "org.jetbrains.kotlin.tools.NativePlugin"
        }
        create("nativeDependenciesDownloader") {
            id = "native-dependencies-downloader"
            implementationClass = "org.jetbrains.kotlin.dependencies.NativeDependenciesDownloaderPlugin"
        }
        create("nativeDependencies") {
            id = "native-dependencies"
            implementationClass = "org.jetbrains.kotlin.dependencies.NativeDependenciesPlugin"
        }
        create("platformManager") {
            id = "platform-manager"
            implementationClass = "org.jetbrains.kotlin.PlatformManagerPlugin"
        }
    }
}
