allprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}

plugins {
    kotlin("multiplatform") apply false
}
