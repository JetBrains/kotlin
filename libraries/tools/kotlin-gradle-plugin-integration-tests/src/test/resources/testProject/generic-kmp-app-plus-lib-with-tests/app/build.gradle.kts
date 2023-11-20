plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js()
    <SingleNativeTarget>("native")
}

kotlin {
    js {
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":lib"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
