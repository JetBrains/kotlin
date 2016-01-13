package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.ConfigurationContainer
import java.net.URL
import org.gradle.api.logging.Logging
import java.util.Properties
import java.io.FileNotFoundException
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import java.lang.reflect.Method
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import java.net.URLClassLoader

// TODO: simplify: the complicated structure is a leftover from dynamic loading of plugin core, could be significantly simplified now

abstract class KotlinBasePluginWrapper: Plugin<Project> {

    val log = Logging.getLogger(this.javaClass)

    public override fun apply(project: Project) {

        val startMemory = getUsedMemoryKb()

        val sourceBuildScript = findSourceBuildScript(project)
        if (sourceBuildScript == null) {
            log.error("Failed to determine source cofiguration of kotlin plugin. Can not download core. Please verify that this or any parent project " +
                    "contains 'kotlin-gradle-plugin' in buildscript's classpath configuration.")
            return
        }

        val kotlinPluginVersion = loadKotlinVersionFromResource(log)
        project.extensions.extraProperties?.set("kotlin.gradle.plugin.version", kotlinPluginVersion)

        val plugin = getPlugin(this.javaClass.classLoader, sourceBuildScript)
        plugin.apply(project)

        project.gradle.addBuildListener(FinishBuildListener(this.javaClass.classLoader, startMemory))
    }

    protected abstract fun getPlugin(pluginClassLoader: ClassLoader, scriptHandler: ScriptHandler): Plugin<Project>

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
    override fun getPlugin(pluginClassLoader: ClassLoader, scriptHandler: ScriptHandler) = KotlinPlugin(scriptHandler, KotlinTasksProvider(pluginClassLoader))
}

open class KotlinAndroidPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(pluginClassLoader: ClassLoader, scriptHandler: ScriptHandler) = KotlinAndroidPlugin(scriptHandler, KotlinTasksProvider(pluginClassLoader))
}

open class Kotlin2JsPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(pluginClassLoader: ClassLoader, scriptHandler: ScriptHandler) = Kotlin2JsPlugin(scriptHandler, KotlinTasksProvider(pluginClassLoader))
}

fun Logger.kotlinDebug(message: String) {
    this.debug("[KOTLIN] $message")
}
