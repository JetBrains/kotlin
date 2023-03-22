plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        compilations["main"].dependencies {
            compileOnly(kotlinStdlib())
        }
    }
    js(IR) {
        compilations["main"].dependencies {
            compileOnly(kotlinStdlib("js"))
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":sample-mpp-lib"))
            }
        }

        val jsMain by getting {
        }
    }
}
