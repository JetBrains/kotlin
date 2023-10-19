import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    sourceSets.all {
        languageSettings {
            optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }
    }

    val platformTarget = when {
        HostManager.hostIsMac -> macosX64("platform")
        HostManager.hostIsMingw -> mingwX64("platform")
        HostManager.hostIsLinux -> linuxX64("platform")
        else -> error("Unexpected host: ${HostManager.host}")
    }

    platformTarget.compilations["main"].cinterops.create("sampleInterop") {
        header(file("src/nativeInterop/cinterop/sampleInterop.h"))
    }

    val platformTest by sourceSets.getting
    val nativeTest = sourceSets.create("nativeTest")
    platformTest.dependsOn(nativeTest)

    tasks.create("listNativeTestDependencies") {
        nativeTest as DefaultKotlinSourceSet
        val nativeTestMetadataConfiguration = configurations[nativeTest.intransitiveMetadataConfigurationName]
        dependsOn(nativeTestMetadataConfiguration)

        doFirst {
            nativeTestMetadataConfiguration.files.forEach { dependencyFile ->
                logger.quiet("Dependency: ${dependencyFile.path}")
            }
        }
    }
}
