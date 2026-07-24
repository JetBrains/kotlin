@file:Suppress("OPT_IN_USAGE")

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js().browser()
    linuxX64()
    linuxArm64()

    val commonMain = sourceSets.getByName("commonMain")
    val nativeMain = sourceSets.create("nativeMain")
    val jvmAndJsMain = sourceSets.create("jvmAndJsMain")
    val jvmMain = sourceSets.getByName("jvmMain")
    val jsMain = sourceSets.getByName("jsMain")
    val linuxX64Main = sourceSets.getByName("linuxX64Main")
    val linuxArm64Main = sourceSets.getByName("linuxArm64Main")

    commonMain.let {
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

    commonMain.dependencies {
        implementation("org.jetbrains.sample:producer:1.0.0")
    }
}
