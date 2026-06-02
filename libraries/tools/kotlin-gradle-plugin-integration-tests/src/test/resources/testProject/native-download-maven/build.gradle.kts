import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    val nativeMain = sourceSets.create("nativeMain") {
        dependsOn(sourceSets["commonMain"])
    }

    targets.withType(KotlinNativeTarget::class.java).all {
        compilations["main"].defaultSourceSet.dependsOn(nativeMain)
    }

    <SingleNativeTarget>("host") {
        binaries {
            executable()
        }
    }
}