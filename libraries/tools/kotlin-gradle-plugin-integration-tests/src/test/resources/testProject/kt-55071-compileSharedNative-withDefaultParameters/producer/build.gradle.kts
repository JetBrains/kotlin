@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

kotlin {
    jvm()
    js().browser()
    linuxX64()
    linuxArm64()

    val commonMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val secondCommonMain by sourceSets.creating
    val jvmAndJsMain by sourceSets.creating
    val jvmMain by sourceSets.getting
    val jsMain by sourceSets.getting
    val linuxX64Main by sourceSets.getting
    val linuxArm64Main by sourceSets.getting

    commonMain.let {
        secondCommonMain.dependsOn(it)
    }

    secondCommonMain.let {
        nativeMain.dependsOn(it)
        jvmAndJsMain.dependsOn(it)
    }

    nativeMain.let {
        linuxArm64Main.dependsOn(it)
        linuxX64Main.dependsOn(it)
    }

    jvmAndJsMain.let {
        jvmMain.dependsOn(it)
        jsMain.dependsOn(it)
    }
}



group = "org.jetbrains.sample"
version = "1.0.0"

publishing {
    repositories {
        maven("<localRepo>")
    }
}