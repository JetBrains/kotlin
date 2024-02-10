plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

kotlin {
    linuxX64().compilations.getByName("main").cinterops.create("dep")
    linuxArm64().compilations.getByName("main").cinterops.create("dep")
}

publishing {
    repositories {
        maven {
            name = "build"
            url = uri("<localRepo>")
        }
    }
}

