plugins {
    kotlin("multiplatform")
}

kotlin {
    iosX64()
    @Suppress("DEPRECATION_ERROR")
    iosArm32()
    linuxX64()
    linuxArm64()
    mingwX64("windowsX64")
    @Suppress("DEPRECATION_ERROR")
    mingwX86("windowsX86")

    val commonMain by sourceSets.getting
    val iosMain by sourceSets.creating
    val linuxMain by sourceSets.creating
    val windowsMain by sourceSets.creating

    val iosX64Main by sourceSets.getting
    val iosArm32Main by sourceSets.getting
    val linuxX64Main by sourceSets.getting
    val linuxArm64Main by sourceSets.getting
    val windowsX64Main by sourceSets.getting
    val windowsX86Main by sourceSets.getting

    iosMain.dependsOn(commonMain)
    linuxMain.dependsOn(commonMain)
    windowsMain.dependsOn(commonMain)

    iosX64Main.dependsOn(iosMain)
    iosArm32Main.dependsOn(iosMain)

    linuxX64Main.dependsOn(linuxMain)
    linuxArm64Main.dependsOn(linuxMain)

    windowsX64Main.dependsOn(windowsMain)
    windowsX86Main.dependsOn(windowsMain)
}
