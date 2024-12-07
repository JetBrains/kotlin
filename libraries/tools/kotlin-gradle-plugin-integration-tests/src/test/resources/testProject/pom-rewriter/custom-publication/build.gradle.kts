version = "3.2.1"

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    jvm()
    macosArm64()
}

publishing {
    publications.named<MavenPublication>("jvm")  {
        this.groupId = "fake-group"
        this.artifactId = "fake-id"
        this.version = "fake-version"
    }
}