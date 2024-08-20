
import org.jetbrains.kotlin.com.intellij.openapi.util.SystemInfo.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

plugins {
    kotlin("multiplatform") apply true
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    val nativePlatform = when {
        isMac -> macosX64("nativePlatform")
        isLinux -> linuxX64("nativePlatform")
        isWindows -> mingwX64("nativePlatform")
        else -> throw IllegalStateException("Unsupported host")
    }

    val commonMain by sourceSets.getting
    val nativePlatformMain by sourceSets.getting
    val nativeMain by sourceSets.creating

    nativeMain.dependsOn(commonMain)
    nativePlatformMain.dependsOn(nativeMain)

    nativePlatform.compilations.getByName("main").cinterops.create("dummy") {
        headers("libs/include/dummy.h")
        compilerOpts.add("-Ilibs/include")
    }
}

fun createListDependenciesTask(sourceSetName: String) {
    tasks.create("list${sourceSetName.capitalize()}Dependencies") {
        val sourceSet = kotlin.sourceSets[sourceSetName] as DefaultKotlinSourceSet
        val metadataFiles = project.configurations[sourceSet.intransitiveMetadataConfigurationName].incoming.artifacts.artifactFiles
        dependsOn(metadataFiles)
        dependsOn("cinteropDummyNativePlatform")
        doFirst {
            metadataFiles.forEach { dependencyFile ->
                logger.quiet("Dependency: $dependencyFile")
            }
        }
    }
}

createListDependenciesTask("nativePlatformMain")
createListDependenciesTask("nativeMain")
createListDependenciesTask("commonMain")