plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets.getByName("commonMain").dependencies {
        implementation("org.jetbrains.kotlin.tests:preHmppLibrary:0.1")
    }
}
