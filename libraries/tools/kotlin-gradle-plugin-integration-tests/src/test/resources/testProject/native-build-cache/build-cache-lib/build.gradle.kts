group = "com.example"
version = "1.0"

plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
    id("maven-publish")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    linuxX64("host")
}

publishing {
    repositories {
        maven(url = "file://${projectDir.absolutePath.replace('\\', '/')}/../repo")
    }
}
