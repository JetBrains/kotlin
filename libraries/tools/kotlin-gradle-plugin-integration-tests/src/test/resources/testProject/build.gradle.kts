
plugins {
    id("org.jetbrains.kotlin.test.fixes.android")

    id("multiplatform")
    `maven-publish`
}

group = "org.sample.kt-62515"
version = 1.0

val repo: File = file("/var/folders/q8/48_9m22114j5bnyj3vwqd9_80000kt/T/junit3978468624971142152/local-repo")

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
