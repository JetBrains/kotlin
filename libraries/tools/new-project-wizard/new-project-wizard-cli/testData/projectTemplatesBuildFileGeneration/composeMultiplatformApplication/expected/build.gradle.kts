buildscript {
    repositories {
        gradlePluginPortal()
        jcenter()
        google()
        maven { url = uri("KOTLIN_REPO") }
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:KOTLIN_VERSION")
        classpath("com.android.tools.build:gradle:4.0.1")
    }
}

group = "me.user"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        jcenter()
        mavenCentral()
        maven { url = uri("KOTLIN_REPO") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}