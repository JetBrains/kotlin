plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets.jvmMain.dependencies {
        implementation("test:producer:1.0")
    }

    sourceSets.jvmTest.dependencies {
        implementation("test:producer:1.0") {
            capabilities {
                requireCapability("test:foo:1.0")
            }
        }
    }
}
