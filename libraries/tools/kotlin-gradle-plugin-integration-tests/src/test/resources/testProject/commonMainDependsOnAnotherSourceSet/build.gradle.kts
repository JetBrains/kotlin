plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        val commonMain by getting
        if (!hasProperty("commonSourceSetDependsOnNothing")) {
            val grandCommonMain by creating
            commonMain.dependsOn(grandCommonMain)
        }
    }
}
