plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    sourceSets {
        getByName("commonMain").dependencies {
            implementation("a:dep-with-cinterop:1.0")
        }
    }
}
