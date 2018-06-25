package org.jetbrains.kotlin.gradle.plugin.experimental.plugins

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformImplementationPluginBase

class KotlinPlatformNativePlugin : KotlinPlatformImplementationPluginBase("native") {

    override fun apply(project: Project) = with(project) {
        pluginManager.apply(KotlinNativePlugin::class.java)
        // This configuration is necessary for correct work of the base platform plugin.
        configurations.create("compile")
        super.apply(project)
    }

    override fun addCommonSourceSetToPlatformSourceSet(commonSourceSet: SourceSet, platformProject: Project) {
        val platformSourceSet = platformProject.kotlinNativeSourceSets.findByName(commonSourceSet.name)
        val commonSources = commonSourceSet.kotlin
        if (platformSourceSet != null && commonSources != null) {
            platformSourceSet.commonSources.from(commonSources)
        } else {
            platformProject.logger.warn("""
                Cannot add a common source set '${commonSourceSet.name}' in the project '${platformProject.path}'.
                No such platform source set or the common source set has no Kotlin sources.
            """.trimIndent())
        }
    }

    override fun namedSourceSetsContainer(project: Project) = project.kotlinNativeSourceSets
}
