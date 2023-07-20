plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        val myCommonMain by creating

        val commonMain by getting {
            dependsOn(myCommonMain)
        }
    }
}
