plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    linuxArm64()

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        }

        val native1Main by sourceSets.creating { dependsOn(commonMain.get()) }
        val native2Main by sourceSets.creating { dependsOn(commonMain.get()) }
        val linuxMain by sourceSets.creating { dependsOn(native1Main); dependsOn(native2Main); }
        sourceSets.getByName("linuxX64Main") { dependsOn(linuxMain) }
        sourceSets.getByName("linuxArm64Main") { dependsOn(linuxMain) }
    }
}

