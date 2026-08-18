/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

rootProject.name = "dokka"

pluginManagement {
    includeBuild("build-logic")
    includeBuild("build-settings-logic")

    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/plugins.gradle.org/m2") {
            name = "GradlePluginPortal-JBCache"
        }
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral {
            setUrl("https://cache-redirector.jetbrains.com/maven-central")
            name = "MavenCentral-JBCache"
        }

        maven("https://cache-redirector.jetbrains.com/intellij-repository/releases") {
            name = "IjRepository-JBCache"
        }
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies") {
            name = "IjDependencies-JBCache"
        }

        //region Declare the Node.js & Yarn download repositories
        // Required by Gradle Node plugin: https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
        exclusiveContent {
            forRepository {
                ivy("https://cache-redirector.jetbrains.com/nodejs.org/dist/") {
                    name = "Nodejs-JBCache"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }

        exclusiveContent {
            forRepository {
                ivy("https://cache-redirector.jetbrains.com/github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn-JBCache"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }
        //endregion
    }
}

plugins {
    id("dokkasettings")
}

// OVERWRITING CATALOG VERSIONS
// for testing against the latest dev version of Analysis API
// currently, Analysis API is used only in the analysis-kotlin-symbols project
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val kotlinCompilerK2Version = providers.gradleProperty(
                "org.jetbrains.dokka.build.overrideAnalysisAPIVersion"
            ).orNull
            if (kotlinCompilerK2Version != null) {
                logger.lifecycle("Using the override version $kotlinCompilerK2Version of Analysis API")
                version("kotlin-compiler-k2", kotlinCompilerK2Version)
            }
        }
    }
}
