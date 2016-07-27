package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.internal.KotlinSourceSetProviderImpl
import org.jetbrains.kotlin.gradle.tasks.AndroidTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import javax.inject.Inject

// TODO: simplify: the complicated structure is a leftover from dynamic loading of plugin core, could be significantly simplified now

abstract class KotlinBasePluginWrapper(protected val fileResolver: FileResolver): Plugin<Project> {
    private val log = Logging.getLogger(this.javaClass)

    override fun apply(project: Project) {
        // TODO: consider only set if if daemon or parallel compilation are enabled, though this way it should be safe too
        System.setProperty(org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        val kotlinPluginVersion = loadKotlinVersionFromResource(log)
        project.extensions.extraProperties?.set("kotlin.gradle.plugin.version", kotlinPluginVersion)

        val plugin = getPlugin()
        plugin.apply(project)

        val cleanUpBuildListener = CleanUpBuildListener(project)
        cleanUpBuildListener.buildStarted()
        project.gradle.addBuildListener(cleanUpBuildListener)
    }

    protected abstract fun getPlugin(): Plugin<Project>
}

open class KotlinPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin() =
            KotlinPlugin(KotlinTasksProvider(), KotlinSourceSetProviderImpl(fileResolver))
}

open class KotlinAndroidPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin() =
            KotlinAndroidPlugin(AndroidTasksProvider(), KotlinSourceSetProviderImpl(fileResolver))
}

open class Kotlin2JsPluginWrapper @Inject constructor(fileResolver: FileResolver): KotlinBasePluginWrapper(fileResolver) {
    override fun getPlugin() =
            Kotlin2JsPlugin(KotlinTasksProvider(), KotlinSourceSetProviderImpl(fileResolver))
}

