plugins {
    kotlin("multiplatform")
}

kotlin {
    iosX64()
    iosArm64()
    linuxX64()
    linuxArm64()
    mingwX64("windowsX64")

    val commonMain by sourceSets.getting
    val iosMain by sourceSets.creating
    val linuxMain by sourceSets.creating

    val iosX64Main by sourceSets.getting
    val iosArm64Main by sourceSets.getting
    val linuxX64Main by sourceSets.getting
    val linuxArm64Main by sourceSets.getting

    iosMain.dependsOn(commonMain)
    linuxMain.dependsOn(commonMain)

    iosX64Main.dependsOn(iosMain)
    iosArm64Main.dependsOn(iosMain)

    linuxX64Main.dependsOn(linuxMain)
    linuxArm64Main.dependsOn(linuxMain)
}
