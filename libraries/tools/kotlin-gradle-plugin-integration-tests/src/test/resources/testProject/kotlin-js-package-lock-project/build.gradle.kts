plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

version = "1.0.0-SNAPSHOT"

kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
            }
        }

        jsTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        useCommonJs()
        binaries.executable()
        nodejs {
        }
    }
}