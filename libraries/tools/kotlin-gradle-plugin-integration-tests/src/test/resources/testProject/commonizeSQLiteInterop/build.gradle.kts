plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
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
    }

    targetA.compilations.getByName("main").cinterops.create("sqlite")
    targetB.compilations.getByName("main").cinterops.create("sqlite")
}
