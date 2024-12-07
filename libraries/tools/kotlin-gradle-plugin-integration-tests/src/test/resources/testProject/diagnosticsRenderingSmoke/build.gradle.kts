plugins {
    kotlin("multiplatform").apply(false)
}

allprojects {
    repositories {
        mavenLocal()
        maven("<localRepo>")
        mavenCentral()
    }
}
