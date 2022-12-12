plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<kgp_version>")
}

repositories {
    mavenLocal()
    mavenCentral()
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("included-build:included")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

