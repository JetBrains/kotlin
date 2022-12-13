plugins {
    kotlin("js")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    js {
        browser {
        }
    }
}
