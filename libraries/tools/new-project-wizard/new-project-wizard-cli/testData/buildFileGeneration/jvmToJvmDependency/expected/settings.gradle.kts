pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("KOTLIN_REPO")
    }

}
rootProject.name = "generatedProject"


include(":a")
include(":b")
include(":c")
include(":d")