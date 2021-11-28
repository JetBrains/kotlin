plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>").apply(false)
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}
