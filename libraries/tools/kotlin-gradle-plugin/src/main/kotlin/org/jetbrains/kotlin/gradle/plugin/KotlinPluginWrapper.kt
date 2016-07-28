package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.tasks.AndroidTasksProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

// TODO: simplify: the complicated structure is a leftover from dynamic loading of plugin core, could be significantly simplified now

abstract class KotlinBasePluginWrapper: Plugin<Project> {

    val log = Logging.getLogger(this.javaClass)

    override fun apply(project: Project) {
        val sourceBuildScript = findSourceBuildScript(project)
        if (sourceBuildScript == null) {
            log.error("Failed to determine source cofiguration of kotlin plugin. Can not download core. Please verify that this or any parent project " +
                    "contains 'kotlin-gradle-plugin' in buildscript's classpath configuration.")
            return
        }
        // TODO: consider only set if if daemon or parallel compilation are enabled, though this way it should be safe too
        System.setProperty(org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

        val kotlinPluginVersion = loadKotlinVersionFromResource(log)
        project.extensions.extraProperties?.set("kotlin.gradle.plugin.version", kotlinPluginVersion)

        val plugin = getPlugin(sourceBuildScript)
        plugin.apply(project)

        val cleanUpBuildListener = CleanUpBuildListener(project)
        cleanUpBuildListener.buildStarted()
        project.gradle.addBuildListener(cleanUpBuildListener)
    }

    protected abstract fun getPlugin(scriptHandler: ScriptHandler): Plugin<Project>

    private fun findSourceBuildScript(project: Project): ScriptHandler? {
        log.kotlinDebug("Looking for proper script handler")
        var curProject = project
        while (curProject != curProject.parent) {
            log.kotlinDebug("Looking in project $project")
            val scriptHandler = curProject.buildscript
            val found = scriptHandler.configurations.findByName("classpath")?.firstOrNull { it.name.contains("kotlin-gradle-plugin") } != null
            if (found) {
                log.kotlinDebug("Found! returning...")
                return scriptHandler
            }
            log.kotlinDebug("not found, switching to parent")
            curProject = curProject.parent ?: break
        }
        return null
    }
}

open class KotlinPluginWrapper: KotlinBasePluginWrapper() {
    override fun getPlugin(scriptHandler: ScriptHandler) = KotlinPlugin(scriptHandler, KotlinTasksProvider())
}

open class KotlinAndroidPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(scriptHandler: ScriptHandler) = KotlinAndroidPlugin(scriptHandler, AndroidTasksProvider())
}

open class Kotlin2JsPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(scriptHandler: ScriptHandler) = Kotlin2JsPlugin(scriptHandler, KotlinTasksProvider())
}

fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}

inline fun Logger.kotlinDebug(message: () -> String) {
    if (isDebugEnabled) {
        kotlinDebug(message())
    }
}
