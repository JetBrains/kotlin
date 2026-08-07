/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

buildscript {
    @Suppress("DEPRECATION")
    val rootBuildDirectory by extra(project.file("../.."))
    apply(from = rootBuildDirectory.resolve("kotlin-native/gradle/loadRootProperties.gradle"))

    dependencies {
        classpath(libs.gson)
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm")
    `kotlin-dsl`
}

dependencies {
    api(gradleApi())

    implementation("org.jetbrains.kotlin:kotlin-reflect:$embeddedKotlinVersion") { isTransitive = false }
    implementation(kotlinBuildHelpers())
    implementation("org.jetbrains.kotlin:kotlin-native-utils:${project.bootstrapKotlinVersion}")

    // To build Konan Gradle plugin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")

    implementation(libs.gson)

    implementation("org.jetbrains.kotlin:kotlin-util-klib:${project.bootstrapKotlinVersion}")
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir("src/main/kotlin")
        }
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    compilerVersion = embeddedKotlinVersion
    coreLibrariesVersion = embeddedKotlinVersion
    jvmToolchain(17)

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

gradlePlugin {
    plugins {
        create("compileToBitcode") {
            id = "compile-to-bitcode"
            implementationClass = "org.jetbrains.kotlin.bitcode.CompileToBitcodePlugin"
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
        create("gitClangFormat") {
            id = "git-clang-format"
            implementationClass = "org.jetbrains.kotlin.cpp.GitClangFormatPlugin"
        }
    }
}

listOf(
        org.jetbrains.kotlin.gradle.plugin.PLUGIN_CLASSPATH_CONFIGURATION_NAME + "Main",
        "compilePluginsBlocksPluginClasspathElements",
).forEach { confName ->
    project.configurations.named(confName) {
        resolutionStrategy {
            eachDependency {
                if (this.requested.group == "org.jetbrains.kotlin") useVersion(embeddedKotlinVersion)
            }
        }
    }
}
