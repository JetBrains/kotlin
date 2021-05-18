import java.io.File
pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        jcenter()
        mavenCentral()
        gradlePluginPortal()
    }
}

buildscript {
    repositories {
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-build-gradle-plugin:0.0.29")
    }
}

//include("tools")
