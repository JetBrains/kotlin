pluginManagement {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("KOTLIN_REPO") }
    }

}
rootProject.name = "generatedProject"


include(":android")