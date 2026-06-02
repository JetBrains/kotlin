plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        val myCommonMain = create("myCommonMain")

        val commonMain = getByName("commonMain")
        commonMain.dependsOn(myCommonMain)
    }
}
