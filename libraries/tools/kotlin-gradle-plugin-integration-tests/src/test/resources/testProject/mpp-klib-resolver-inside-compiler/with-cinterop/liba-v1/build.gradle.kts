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
}

publishing {
    repositories {
        maven { url = uri(repo) }
    }
}

kotlin {
    macosArm64 {
        compilations.getByName("main") {
            cinterops {
                val liba by creating
            }
        }
    }
    linuxArm64 {
        compilations.getByName("main") {
            cinterops {
                val liba by creating
            }
        }
    }
}
