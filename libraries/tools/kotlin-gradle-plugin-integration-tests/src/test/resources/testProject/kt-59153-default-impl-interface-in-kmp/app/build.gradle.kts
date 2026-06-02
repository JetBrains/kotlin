plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js() {
        nodejs()
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(project(":lib"))
            }
        }
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
