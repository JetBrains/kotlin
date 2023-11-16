plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
}

group = "org.sample.kt-62515"
version = 1.0

val repo: File = file("<localRepo>")

repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri(repo) }
}

publishing {
    repositories {
        maven { url = uri(repo) }
    }
}

kotlin {
    macosArm64()
    linuxArm64()

    sourceSets.commonMain {
        dependencies {
            implementation("org.sample.kt-62515:liba:2.0")
        }
    }
}
