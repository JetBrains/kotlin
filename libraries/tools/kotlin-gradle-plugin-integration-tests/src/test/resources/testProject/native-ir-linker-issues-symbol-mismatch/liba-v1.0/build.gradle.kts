plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "org.sample"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    <SingleNativeTarget>("native")
}

publishing {
    repositories {
        maven {
            url = uri("<localRepo>")
        }
    }
}
