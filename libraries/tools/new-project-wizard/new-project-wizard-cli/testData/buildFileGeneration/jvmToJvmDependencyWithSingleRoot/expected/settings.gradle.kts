pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            url = uri("KOTLIN_REPO")
        }
    }

}
rootProject.name = "generatedProject"


include(":b:c")
include(":b")