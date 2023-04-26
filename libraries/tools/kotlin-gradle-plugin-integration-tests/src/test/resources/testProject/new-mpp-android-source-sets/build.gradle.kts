buildscript {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }

    dependencies {
        classpath(kotlin("gradle-plugin:${property("kotlin_version")}"))
        classpath("com.android.tools.build:gradle:${property("android_tools_version")}")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}
