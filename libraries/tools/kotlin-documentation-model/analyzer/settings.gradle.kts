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

    // subproject :kotlin-analysis:intellij-dependency requires specific repositories that should not be used in
    // the other subprojects, so use PREFER_PROJECT to allow subprojects to override the repositories defined here.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)

    repositories {
        mavenCentral()
        google()

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

include(
    ":core",
    ":core:test-api",
    ":core:content-matcher-test-utils",

    ":kotlin-analysis",
    ":kotlin-analysis:intellij-dependency",
    ":kotlin-analysis:compiler-dependency",

    ":runners:gradle-plugin",
    ":runners:cli",
    ":runners:maven-plugin",

    ":plugins:base",
    ":plugins:base:frontend",
    ":plugins:base:search-component",
    ":plugins:base:base-test-utils",
    ":plugins:all-modules-page",
    ":plugins:templating",
    ":plugins:versioning",
    ":plugins:android-documentation",

    ":plugins:mathjax",
    ":plugins:gfm",
    ":plugins:gfm:gfm-template-processing",
    ":plugins:jekyll",
    ":plugins:jekyll:jekyll-template-processing",
    ":plugins:kotlin-as-java",
    ":plugins:javadoc",

    ":integration-tests",
    ":integration-tests:gradle",
    ":integration-tests:cli",
    ":integration-tests:maven",

    ":mkdocs",
)

val isCiBuild = System.getenv("GITHUB_ACTIONS") != null || System.getenv("TEAMCITY_VERSION") != null



gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(isCiBuild)
    }
}


enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
