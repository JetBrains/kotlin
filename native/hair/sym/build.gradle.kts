plugins {
    kotlin("multiplatform")
    id("common-configuration")
}

kotlin {
    jvm()
    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                api(kotlinStdlib())
            }
        }
    }
}
