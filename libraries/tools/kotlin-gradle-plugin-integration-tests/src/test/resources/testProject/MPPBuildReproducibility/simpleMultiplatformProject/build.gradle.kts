plugins {
    kotlin("multiplatform")
    `maven-publish`
}

version = "1.0"
group = "org.jetbrains"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    js { browser() }
    linuxX64()
    linuxArm64()

    val commonMain by sourceSets.getting
    val linuxX64Main by sourceSets.getting
    val linuxArm64Main by sourceSets.getting
    val nativeMain by sourceSets.creating

    nativeMain.dependsOn(commonMain)
    linuxX64Main.dependsOn(nativeMain)
    linuxArm64Main.dependsOn(nativeMain)
}
