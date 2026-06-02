plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
    jvm()

    sourceSets {
        val myCustomSourceSet = create("myCustomSourceSet")
        commonMain.get().dependsOn(myCustomSourceSet)
    }
}
