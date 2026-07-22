plugins {
    kotlin("multiplatform")
}

kotlin {
    // targets do not matter just need Kotlin MPP Plugin
    jvm()
    linuxX64()

    sourceSets {
        val myCustomSourceSet = create("myCustomSourceSet")

        // check that usual diagnostics are not deduplicated even if they are exactly the same
        val commonMain = getByName("commonMain")
        commonMain.dependsOn(myCustomSourceSet)
    }
}
