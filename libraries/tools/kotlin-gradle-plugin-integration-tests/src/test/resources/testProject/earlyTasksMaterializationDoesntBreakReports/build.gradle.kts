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
        val myCommonMain by creating

        val commonMain by getting {
            dependsOn(myCommonMain)
        }
    }
}
