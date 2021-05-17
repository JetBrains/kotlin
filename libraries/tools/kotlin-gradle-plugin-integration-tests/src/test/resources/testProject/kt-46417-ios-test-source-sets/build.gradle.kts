allprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
        jcenter()
    }
}

plugins {
    kotlin("multiplatform") apply false
}
