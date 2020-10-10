pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("KOTLIN_REPO")
    }

}
rootProject.name = "generatedProject"


include(":b:c")
include(":b")