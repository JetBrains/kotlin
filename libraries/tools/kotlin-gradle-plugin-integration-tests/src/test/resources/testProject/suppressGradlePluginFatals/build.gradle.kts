plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        val intermediate by creating

        val jvmMain by getting {
            dependsOn(intermediate)
        }

        intermediate.dependsOn(jvmMain)
    }
}