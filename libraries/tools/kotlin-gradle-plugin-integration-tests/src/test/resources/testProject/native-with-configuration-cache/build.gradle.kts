import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val commonMainSourceSet = sourceSets.commonMain.get()
    val nativeMain by sourceSets.creating {
        dependsOn(commonMainSourceSet)
    }

    targets.withType(KotlinNativeTarget::class.java).all {
        compilations["main"].defaultSourceSet.dependsOn(nativeMain)
    }

    <SingleNativeTarget>()
}