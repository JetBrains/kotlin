plugins {
    kotlin("multiplatform")
}

kotlin {
    iosX64()
    iosArm64()
    linuxX64()
    linuxArm64()
    mingwX64("windowsX64")

    val commonMain = sourceSets.getByName("commonMain")
    val iosMain = sourceSets.create("iosMain")
    val linuxMain = sourceSets.create("linuxMain")

    val iosX64Main = sourceSets.getByName("iosX64Main")
    val iosArm64Main = sourceSets.getByName("iosArm64Main")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")

    iosMain.dependsOn(commonMain)
    linuxMain.dependsOn(commonMain)

    iosX64Main.dependsOn(iosMain)
    iosArm64Main.dependsOn(iosMain)

    linuxX64Main.dependsOn(linuxMain)
    linuxArm64Main.dependsOn(linuxMain)
}
