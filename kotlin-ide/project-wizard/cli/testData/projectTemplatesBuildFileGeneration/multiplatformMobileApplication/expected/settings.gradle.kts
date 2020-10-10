pluginManagement {
    repositories {
        google()
        jcenter()
        gradlePluginPortal()
        mavenCentral()
        maven("KOTLIN_REPO")
    }

}
rootProject.name = "multiplatformMobileApplication"


include(":androidApp")
include(":shared")