plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    jvm()

    sourceSets {
        val myCustomSourceSet by creating
        commonMain.get().dependsOn(myCustomSourceSet)
    }
}
