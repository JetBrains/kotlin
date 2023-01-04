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

kotlin {

    val nativePlatform = when {
        isMac -> macosX64("nativePlatform")
        isLinux -> linuxX64("nativePlatform")
        isWindows -> mingwX64("nativePlatform")
        else -> throw IllegalStateException("Unsupported host")
    }

    jvm()

    val commonMain by sourceSets.getting
    val jvmMain by sourceSets.getting
    val nativePlatformMain by sourceSets.getting

    commonMain {
        -jvmMain
        -nativePlatformMain
    }

    tasks.register("listNativePlatformMainDependencies") {
        doLast {
            val intransitiveMetadataConfigurationDependencies = project.configurations.findByName(
                "nativePlatformMainIntransitiveDependenciesMetadata"
            )?.files.orEmpty()

            intransitiveMetadataConfigurationDependencies.forEach { dependencyFile ->
                logger.quiet("intransitiveMetadataConfiguration: ${dependencyFile.path}")
            }
        }
    }
}
