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

    val commonMain = sourceSets.getByName("commonMain")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
    val nativeMain = sourceSets.create("nativeMain")

    nativeMain.dependsOn(commonMain)
    linuxX64Main.dependsOn(nativeMain)
    linuxArm64Main.dependsOn(nativeMain)
}
