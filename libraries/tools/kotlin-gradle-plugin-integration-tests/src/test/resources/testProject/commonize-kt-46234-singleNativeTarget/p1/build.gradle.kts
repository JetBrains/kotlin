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

    val commonMain by sourceSets.getting
    val nativeMain by sourceSets.creating
    val nativePlatformMain by sourceSets.getting

    commonMain {
        -nativeMain {
            -nativePlatformMain
        }
    }

    tasks.register("listNativePlatformMainDependencies") {
        doLast {

            val implementationMetadataConfigurationDependencies = project.configurations.getByName(
                nativePlatformMain.implementationMetadataConfigurationName
            ).files

            val intransitiveMetadataConfigurationDependencies = project.configurations.findByName(
                "nativePlatformMainIntransitiveDependenciesMetadata"
            )?.files.orEmpty()

            val dependencies = implementationMetadataConfigurationDependencies +
                    intransitiveMetadataConfigurationDependencies

            logger.quiet("NativePlatformMainDependency | Count: ${dependencies.size}")

            implementationMetadataConfigurationDependencies.forEach { dependencyFile ->
                logger.quiet("implementationMetadataConfiguration: ${dependencyFile.path}")
            }

            intransitiveMetadataConfigurationDependencies.forEach { dependencyFile ->
                logger.quiet("intransitiveMetadataConfiguration: ${dependencyFile.path}")
            }
        }
    }
}
