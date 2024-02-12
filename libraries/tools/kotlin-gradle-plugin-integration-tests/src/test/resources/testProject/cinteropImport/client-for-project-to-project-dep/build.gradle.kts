plugins {
    kotlin("multiplatform")
    id("maven-publish")
}

allprojects {
    group = "a"
    version = "1.0"
}

kotlin {
    linuxX64()
    linuxArm64()

    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":dep-with-cinterop"))
    }
}
