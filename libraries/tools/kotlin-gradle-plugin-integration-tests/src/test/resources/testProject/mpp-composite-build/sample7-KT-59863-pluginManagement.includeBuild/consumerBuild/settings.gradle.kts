rootProject.name = "consumerBuild"

rootProject.name = "consumer"

pluginManagement {
    includeBuild("<producer_path>")
}

plugins {
    id("org.jetbrains.example.gradle.plugin")
}
