rootProject.name = "dokka"

include("core")
include("core:test-api")
include("core:content-matcher-test-utils")

include("kotlin-analysis")
include("kotlin-analysis:intellij-dependency")
include("kotlin-analysis:compiler-dependency")

include("runners:gradle-plugin")
include("runners:cli")
include("runners:maven-plugin")

include("plugins:base")
include("plugins:base:frontend")
include("plugins:base:search-component")
include("plugins:base:base-test-utils")
include("plugins:all-modules-page")
include("plugins:templating")
include("plugins:versioning")
include("plugins:android-documentation")

include("plugins:mathjax")
include("plugins:gfm")
include("plugins:gfm:gfm-template-processing")
include("plugins:jekyll")
include("plugins:jekyll:jekyll-template-processing")
include("plugins:kotlin-as-java")
include("plugins:javadoc")

include("integration-tests")
include("integration-tests:gradle")
include("integration-tests:cli")
include("integration-tests:maven")

include("test-utils")

include("mkdocs")

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("com.gradle.plugin-publish") version "0.20.0"
    }
}

val isCiBuild = System.getenv("GITHUB_ACTIONS") != null || System.getenv("TEAMCITY_VERSION") != null

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(isCiBuild)
    }
}
