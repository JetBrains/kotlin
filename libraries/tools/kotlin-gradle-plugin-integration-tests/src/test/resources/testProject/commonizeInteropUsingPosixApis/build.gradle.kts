plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm() // https://youtrack.jetbrains.com/issue/KT-45832
    val targetA = <targetA>("targetA")
    val targetB = <targetB>("targetB")

    val commonMain = sourceSets.getByName("commonMain")
    val nativeMain = sourceSets.create("nativeMain")
    val targetAMain = sourceSets.getByName("targetAMain")
    val targetBMain = sourceSets.getByName("targetBMain")

    nativeMain.dependsOn(commonMain)
    targetAMain.dependsOn(nativeMain)
    targetBMain.dependsOn(nativeMain)

    sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }

    targetA.compilations.getByName("main").cinterops.create("withPosix") {
        header(file("libs/withPosix.h"))
    }
    targetB.compilations.getByName("main").cinterops.create("withPosix") {
        header(file("libs/withPosix.h"))
    }
}
