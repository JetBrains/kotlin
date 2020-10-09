pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        jcenter()
        maven {
            url = uri("KOTLIN_REPO")
        }
    }

}
rootProject.name = "generatedProject"


include(":android")