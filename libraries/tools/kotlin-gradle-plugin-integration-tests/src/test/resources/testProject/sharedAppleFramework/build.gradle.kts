buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin:${property("kotlin_version")}"))
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}