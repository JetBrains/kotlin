plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.sample.kt-73511"
version = 1.0

publishing {
    repositories {
        maven { url = uri("<localRepo>") }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("<localRepo>") }
}

kotlin {
    macosArm64()

    sourceSets.commonMain.dependencies {
        implementation("org.sample.kt-73511:lib1:1.0")
    }
}
