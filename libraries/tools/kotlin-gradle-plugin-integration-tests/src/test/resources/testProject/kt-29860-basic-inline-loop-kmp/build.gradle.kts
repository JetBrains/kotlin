plugins {
    kotlin("multiplatform")
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }
}
