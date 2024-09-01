plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxArm64()
    linuxX64()

    val commonMain by sourceSets.getting
    val linuxMain by sourceSets.creating
    val linuxArm64Main by sourceSets.getting
    val linuxX64Main by sourceSets.getting

    linuxMain.dependsOn(commonMain)
    linuxArm64Main.dependsOn(linuxMain)
    linuxX64Main.dependsOn(linuxMain)
}
