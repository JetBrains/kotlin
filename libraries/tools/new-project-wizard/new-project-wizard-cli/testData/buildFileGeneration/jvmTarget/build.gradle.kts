plugins {
    kotlin("multiplatform") version "KOTLIN_VERSION"
}
group = "testGroupId"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}
kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    sourceSets {
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}