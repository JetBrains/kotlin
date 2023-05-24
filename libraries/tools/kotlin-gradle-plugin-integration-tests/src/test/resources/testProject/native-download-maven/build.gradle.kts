import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
}

repositories {
    // <MavenPlaceholder>
    mavenCentral()
    mavenLocal()
}

kotlin {
    val nativeMain by sourceSets.creating {
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