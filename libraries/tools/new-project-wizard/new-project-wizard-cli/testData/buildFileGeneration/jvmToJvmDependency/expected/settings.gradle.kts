pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("KOTLIN_REPO")
        }
    }

}
rootProject.name = "generatedProject"


include(":a")
include(":b")
include(":c")
include(":d")