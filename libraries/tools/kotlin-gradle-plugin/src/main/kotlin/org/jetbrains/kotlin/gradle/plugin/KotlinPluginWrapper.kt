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
import java.lang.reflect.Method
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

abstract class KotlinBasePluginWrapper: Plugin<Project> {
    val log = Logging.getLogger(this.javaClass)

    public override fun apply(project: Project) {

        val sourceBuildScript = findSourceBuildScript(project)
        if (sourceBuildScript == null) {
            log.error("Failed to determine source cofiguration of kotlin plugin. Can not download core. Please verify that this or any parent project " +
                    "contains 'kotlin-gradle-plugin' in buildscript's classpath configuration.")
            return
        }

        val kotlinPluginVersion = loadKotlinVersionFromResource(log)
        project.getExtensions().getExtraProperties()?.set("kotlin.gradle.plugin.version", kotlinPluginVersion)

        val pluginClassLoader = createPluginIsolatedClassLoader(kotlinPluginVersion, sourceBuildScript)
        val plugin = getPlugin(pluginClassLoader, sourceBuildScript)
        plugin.apply(project)

        project.getGradle().addBuildListener(FinishBuildListener(pluginClassLoader))
    }

    protected abstract fun getPlugin(pluginClassLoader: ParentLastURLClassLoader, scriptHandler: ScriptHandler): Plugin<Project>

    private fun createPluginIsolatedClassLoader(projectVersion: String, sourceBuildScript: ScriptHandler): ParentLastURLClassLoader {
        val dependencyHandler: DependencyHandler = sourceBuildScript.getDependencies()
        val configurationsContainer: ConfigurationContainer = sourceBuildScript.getConfigurations()

        log.debug("Creating configuration and dependency")
        val kotlinPluginCoreCoordinates = "org.jetbrains.kotlin:kotlin-gradle-plugin-core:" + projectVersion
        val dependency = dependencyHandler.create(kotlinPluginCoreCoordinates)
        val configuration = configurationsContainer.detachedConfiguration(dependency)

        log.debug("Resolving [" + kotlinPluginCoreCoordinates + "]")
        val kotlinPluginDependencies: List<URL> = configuration.getResolvedConfiguration().getFiles({ true })!!.map { it.toURI().toURL() }
        log.debug("Resolved files: [" + kotlinPluginDependencies.toString() + "]")
        log.debug("Load plugin in parent-last URL classloader")
        val kotlinPluginClassloader = ParentLastURLClassLoader(kotlinPluginDependencies, this.javaClass.getClassLoader())
        log.debug("Class loader created")

        return kotlinPluginClassloader
    }

    private fun findSourceBuildScript(project: Project): ScriptHandler? {
        log.debug("Looking for proper script handler")
        var curProject = project
        while (curProject != curProject.getParent()) {
            log.debug("Looking in project $project")
            val scriptHandler = curProject.getBuildscript()
            val found = scriptHandler.getConfigurations().findByName("classpath")?.firstOrNull { it.name.contains("kotlin-gradle-plugin") } != null
            if (found) {
                log.debug("Found! returning...")
                return scriptHandler
            }
            log.debug("not found, switching to parent")
            curProject = curProject.getParent()!!
        }
        return null
    }
}

open class KotlinPluginWrapper: KotlinBasePluginWrapper() {
    override fun getPlugin(pluginClassLoader: ParentLastURLClassLoader, scriptHandler: ScriptHandler) = KotlinPlugin(scriptHandler, KotlinTasksProvider(pluginClassLoader))
}

open class KotlinAndroidPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(pluginClassLoader: ParentLastURLClassLoader, scriptHandler: ScriptHandler) = KotlinAndroidPlugin(scriptHandler, KotlinTasksProvider(pluginClassLoader))
}

open class Kotlin2JsPluginWrapper : KotlinBasePluginWrapper() {
    override fun getPlugin(pluginClassLoader: ParentLastURLClassLoader, scriptHandler: ScriptHandler) = Kotlin2JsPlugin(scriptHandler, KotlinTasksProvider(pluginClassLoader))
}

