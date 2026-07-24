plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}


// Materialize tasks early
tasks.all { }

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        val myCommonMain = create("myCommonMain")

        val commonMain = getByName("commonMain")
        commonMain.dependsOn(myCommonMain)
    }
}
