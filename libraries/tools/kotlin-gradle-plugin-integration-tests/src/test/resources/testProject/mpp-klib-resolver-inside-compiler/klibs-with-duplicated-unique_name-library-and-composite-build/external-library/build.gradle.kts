plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
}

group = "org.sample.kt62515.foo"
version = 1.0

repositories {
    mavenLocal()
    mavenCentral()
}

publishing {
    repositories {
        maven { url = uri("<localRepo>") }
    }
}

kotlin {
    jvm()
    js {
        nodejs()
    }
    linuxX64()
}
