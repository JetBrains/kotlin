plugins {
    kotlin("multiplatform") version "{{kotlin_plugin_version}}"
}

repositories {
    {{kts_kotlin_plugin_repositories}}
}

kotlin {
    iosX64("ios") {
        binaries {
            executable()
        }
    }

    watchosX86("watchos") {
        binaries {
            executable()
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }
}
