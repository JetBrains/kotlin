plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        nodejs()
    }
    linuxX64()

    sourceSets.commonMain.dependencies {
        implementation("org.jetbrains.kotlin.kar.test:sample:1.0")
    }
}
