plugins {
    kotlin("multiplatform") version "1.3.60"
}

repositories {
    jcenter()
}

kotlin {
    val jvm = jvm()
    val iosDev = iosArm64()
    val iosSim = iosX64()
    val watchDev = watchosArm32()
    val watchSim = watchosX86()

    sourceSets {
        val commonMain by getting
        val appleMain by creating {
            dependsOn(commonMain)
        }

        val iosMain by creating {
            dependsOn(appleMain)
        }

        configure(listOf(watchDev, watchSim)) {
            compilations["main"].defaultSourceSet.dependsOn(appleMain)
        }

        configure(listOf(iosDev, iosSim)) {
            compilations["main"].defaultSourceSet.dependsOn(iosMain)
        }
    }
}
