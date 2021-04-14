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
include("plugins:kotlin-as-java")
include("plugins:javadoc")

include("integration-tests")
include("integration-tests:gradle")
include("integration-tests:cli")
include("integration-tests:maven")

include("test-utils")

include("docs")

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlin_version
        id("com.github.johnrengelman.shadow") version "5.2.0"
        id("com.jfrog.bintray") version "1.8.5"
        id("com.gradle.plugin-publish") version "0.12.0"
    }

    repositories {
        mavenCentral()
        jcenter()
        gradlePluginPortal()
    }
}
