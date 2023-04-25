plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvm()
    linuxX64()

    sourceSets.getByName("commonMain").dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("kotlin-test"))

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    }
}
