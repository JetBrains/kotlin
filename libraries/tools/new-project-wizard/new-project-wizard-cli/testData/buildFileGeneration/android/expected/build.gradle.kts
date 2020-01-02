buildscript {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:3.2.1")
        classpath(kotlin("gradle-plugin", "1.3.61"))
    }
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}