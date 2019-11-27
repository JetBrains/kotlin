buildscript {
    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
        mavenCentral()
        google()
        mavenLocal()
        jcenter()
    }

    dependencies {
        classpath(kotlin("gradle-plugin:${property("kotlin_version")}"))
        classpath("com.android.tools.build:gradle:${property("android_tools_version")}")
    }
}

allprojects {
    repositories {
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
        mavenCentral()
        google()
        mavenLocal()
        jcenter()
    }
}

