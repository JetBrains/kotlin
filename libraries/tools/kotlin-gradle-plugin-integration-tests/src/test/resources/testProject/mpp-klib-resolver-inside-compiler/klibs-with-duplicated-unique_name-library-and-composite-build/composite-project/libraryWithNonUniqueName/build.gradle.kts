plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

group = "org.sample.kt62515.bar"
version = 1.0

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    js {
        nodejs()
    }
    linuxX64()
}
