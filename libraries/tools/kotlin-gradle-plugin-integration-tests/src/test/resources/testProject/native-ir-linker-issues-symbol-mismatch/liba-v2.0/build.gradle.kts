plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("maven-publish")
}

group = "org.sample"
version = "2.0"

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
