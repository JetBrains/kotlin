plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>").apply(false)
}

allprojects {
    repositories {
        mavenLocal()
        maven("$rootDir/../repo")
        jcenter()
    }
}