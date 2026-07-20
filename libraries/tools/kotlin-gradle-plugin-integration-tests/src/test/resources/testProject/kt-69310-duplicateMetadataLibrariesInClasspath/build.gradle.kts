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

        val native1Main = sourceSets.create("native1Main") { dependsOn(commonMain.get()) }
        val native2Main = sourceSets.create("native2Main") { dependsOn(commonMain.get()) }
        val linuxMain = sourceSets.create("linuxMain") { dependsOn(native1Main); dependsOn(native2Main); }
        sourceSets.getByName("linuxX64Main") { dependsOn(linuxMain) }
        sourceSets.getByName("linuxArm64Main") { dependsOn(linuxMain) }
    }
}
