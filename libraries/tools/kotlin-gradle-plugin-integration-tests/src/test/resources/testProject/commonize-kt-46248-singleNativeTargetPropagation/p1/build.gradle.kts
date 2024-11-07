import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.konan.target.HostManager

operator fun KotlinSourceSet.invoke(builder: SourceSetHierarchyBuilder.() -> Unit): KotlinSourceSet {
    SourceSetHierarchyBuilder(this).builder()
    return this
}

class SourceSetHierarchyBuilder(private val node: KotlinSourceSet) {
    operator fun KotlinSourceSet.unaryMinus() = this.dependsOn(node)
}

plugins {
    kotlin("multiplatform")
}

fun registerListDependenciesTask(sourceSet: KotlinSourceSet) {
    tasks.register("list${sourceSet.name.capitalize()}Dependencies") {
        val dependencyConfiguration = project.configurations.getByName(
            "${sourceSet.name}IntransitiveDependenciesMetadata"
        ).incoming.artifacts.artifactFiles

        val sourceSetName = sourceSet.name

        dependsOn("commonize")
        dependsOn(dependencyConfiguration)

        doLast {
            val dependencies = dependencyConfiguration.files.orEmpty()
            logger.quiet("$sourceSetName Dependencies | Count: ${dependencies.size}")
            dependencies.forEach { dependencyFile ->
                logger.quiet("Dependency: ${dependencyFile.path}")
            }
        }
    }
}

kotlin {
    val nativePlatform = when {
        HostManager.hostIsMac -> macosX64("nativePlatform")
        HostManager.hostIsLinux -> linuxX64("nativePlatform")
        HostManager.hostIsMingw -> mingwX64("nativePlatform")
        else -> throw IllegalStateException("Unsupported host")
    }

    val unsupportedNativePlatform = when {
        HostManager.hostIsMac -> mingwX64("unsupportedNativePlatform")
        else -> macosX64("unsupportedNativePlatform")
    }

    jvm()


    val commonMain by sourceSets.getting
    val jvmMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val nativeMainParent by sourceSets.creating
    val nativePlatformMain by sourceSets.getting
    val unsupportedNativePlatformMain by sourceSets.getting

    commonMain {
        -jvmMain
        -nativeMainParent {
            -nativeMain {
                -nativePlatformMain
                -unsupportedNativePlatformMain
            }
        }
    }

    nativePlatform.compilations.getByName("main").cinterops.create("dummy") {
        headers("libs/include/dummy.h")
        compilerOpts.add("-Ilibs/include")
    }

    unsupportedNativePlatform.compilations.getByName("main").cinterops.create("dummy") {
        headers("libs/include/dummy.h")
        compilerOpts.add("-Ilibs/include")
    }

    registerListDependenciesTask(commonMain)
    registerListDependenciesTask(nativeMain)
    registerListDependenciesTask(nativeMainParent)
}
