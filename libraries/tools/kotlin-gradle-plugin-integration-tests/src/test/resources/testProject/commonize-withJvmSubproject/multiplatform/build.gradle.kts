plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxArm64()
    linuxX64()

    val commonMain = sourceSets.getByName("commonMain")
    val linuxMain = sourceSets.create("linuxMain")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")

    linuxMain.dependsOn(commonMain)
    linuxArm64Main.dependsOn(linuxMain)
    linuxX64Main.dependsOn(linuxMain)
}
