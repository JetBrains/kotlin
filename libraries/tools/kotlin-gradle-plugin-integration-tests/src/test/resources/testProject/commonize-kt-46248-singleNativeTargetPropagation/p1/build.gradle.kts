import org.jetbrains.kotlin.com.intellij.openapi.util.SystemInfo.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

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
        )

        dependsOn("commonize")
        dependsOn(dependencyConfiguration)

        doLast {
            val dependencies = dependencyConfiguration.files.orEmpty()
            logger.quiet("${sourceSet.name} Dependencies | Count: ${dependencies.size}")
            dependencies.forEach { dependencyFile ->
                logger.quiet("Dependency: ${dependencyFile.path}")
            }
        }
    }
}

kotlin {
    val nativePlatform = when {
        isMac -> macosX64("nativePlatform")
        isLinux -> linuxX64("nativePlatform")
        isWindows -> mingwX64("nativePlatform")
        else -> throw IllegalStateException("Unsupported host")
    }

    val unsupportedNativePlatform = when {
        isMac -> mingwX64("unsupportedNativePlatform")
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
