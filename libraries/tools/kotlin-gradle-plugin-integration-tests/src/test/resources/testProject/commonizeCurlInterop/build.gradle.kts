import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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

    val commonMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val targetAMain by sourceSets.getting
    val targetBMain by sourceSets.getting

    nativeMain.dependsOn(commonMain)
    targetAMain.dependsOn(nativeMain)
    targetBMain.dependsOn(nativeMain)

    sourceSets.all {
        languageSettings.optIn("kotlin.RequiresOptIn")
        languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }

    targetA.compilations.getByName("main").cinterops.create("curl")
    targetB.compilations.getByName("main").cinterops.create("curl")

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.all {
            linkerOpts(projectDir.resolve("libcurl.tbd").path)
        }
    }
}
