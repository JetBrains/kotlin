plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

allprojects {
    group = "a"
    version = "1.0"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

kotlin {
    linuxX64()
    linuxArm64()

    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":dep-with-cinterop"))
    }
}
