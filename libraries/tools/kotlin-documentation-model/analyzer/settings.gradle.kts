/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("UnstableApiUsage")

rootProject.name = "dokka"

pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()

        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
        maven("https://cache-redirector.jetbrains.com/maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")

        maven("https://cache-redirector.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-third-party-dependencies")

        // Declare the Node.js & Yarn download repositories
        // Required by Gradle Node plugin: https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
        exclusiveContent {
            forRepository {
                ivy("https://cache-redirector.jetbrains.com/nodejs.org/dist/") {
                    name = "Node Distributions at $url"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("org.nodejs", "node") }
                }
            }
            filter { includeGroup("org.nodejs") }
        }

        exclusiveContent {
            forRepository {
                ivy("https://github.com/yarnpkg/yarn/releases/download") {
                    name = "Yarn Distributions at $url"
                    patternLayout { artifact("v[revision]/[artifact](-v[revision]).[ext]") }
                    metadataSources { artifact() }
                    content { includeModule("com.yarnpkg", "yarn") }
                }
            }
            filter { includeGroup("com.yarnpkg") }
        }
    }
}

plugins {
    `gradle-enterprise`
}

includeBuild("dokka-integration-tests")
includeBuild("dokka-runners/runner-gradle-plugin-classic")
includeBuild("dokka-runners/runner-maven-plugin")
includeBuild("dokka-runners/runner-cli")

include(
    ":docs-developer",

    ":dokka-subprojects",
    ":dokka-subprojects:analysis-java-psi",
    ":dokka-subprojects:analysis-kotlin-api",
    ":dokka-subprojects:analysis-kotlin-descriptors",
    ":dokka-subprojects:analysis-kotlin-descriptors-compiler",
    ":dokka-subprojects:analysis-kotlin-descriptors-ide",
    ":dokka-subprojects:analysis-kotlin-symbols",
    ":dokka-subprojects:analysis-markdown-jb",
    ":dokka-subprojects:core",
    ":dokka-subprojects:core-content-matcher-test-utils",
    ":dokka-subprojects:core-test-api",
    ":dokka-subprojects:plugin-all-modules-page",
    ":dokka-subprojects:plugin-android-documentation",
    ":dokka-subprojects:plugin-base",
    ":dokka-subprojects:plugin-base-frontend",
    ":dokka-subprojects:plugin-base-test-utils",
    ":dokka-subprojects:plugin-gfm",
    ":dokka-subprojects:plugin-gfm-template-processing",
    ":dokka-subprojects:plugin-javadoc",
    ":dokka-subprojects:plugin-jekyll",
    ":dokka-subprojects:plugin-jekyll-template-processing",
    ":dokka-subprojects:plugin-kotlin-as-java",
    ":dokka-subprojects:plugin-mathjax",
    ":dokka-subprojects:plugin-templating",
    ":dokka-subprojects:plugin-versioning",
)

val isCiBuild = System.getenv("GITHUB_ACTIONS") != null || System.getenv("TEAMCITY_VERSION") != null

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(isCiBuild)
    }
}

// This hack is required for included build support.
// The name of the published artifact is `dokka-core`, but the module is named `core`.
// For some reason, dependency substitution doesn't work in this case. Maybe we fall under one of the unsupported
// cases: https://docs.gradle.org/current/userguide/composite_builds.html#included_build_substitution_limitations.
// Should no longer be a problem once Dokka's artifacts are relocated, see #3245.
project(":dokka-subprojects:core").name = "dokka-core"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
