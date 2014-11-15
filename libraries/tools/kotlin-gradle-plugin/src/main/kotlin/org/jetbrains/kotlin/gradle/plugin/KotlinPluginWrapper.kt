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

abstract class KotlinBasePluginWrapper: Plugin<Project> {
    val log = Logging.getLogger(this.javaClass)

    public override fun apply(project: Project) {

        val sourceBuildScript = findSourceBuildScript(project)
        if (sourceBuildScript == null) {
            log.error("Failed to determine source cofiguration of kotlin plugin. Can not download core. Please verify that this or any parent project " +
                    "contains 'kotlin-gradle-plugin' in buildscript's classpath configuration.")
            return
        }

        val kotlinPluginVersion = loadKotlinVersionFromResource()
        project.getExtensions().getExtraProperties()?.set("kotlin.gradle.plugin.version", kotlinPluginVersion)

        val pluginClassLoader = createPluginIsolatedClassLoader(kotlinPluginVersion, sourceBuildScript)

        val cls = Class.forName(getPluginClassName(), true, pluginClassLoader)
        log.debug("Plugin class loaded")

        val constructor = cls.getConstructor(javaClass<ScriptHandler>())
        val method = cls.getMethod("apply", javaClass<Project>())
        log.debug("'apply' method found, invoking...")

        val plugin = constructor.newInstance(sourceBuildScript)
        log.debug("Plugin class instantiated")

        method?.invoke(plugin, project)
        log.debug("'apply' method invoked successfully")

        val gradle = project.javaClass.getMethod("getGradle").invoke(project) as Gradle
        gradle.addBuildListener(FinishBuildListener(pluginClassLoader))
    }

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

    private fun loadKotlinVersionFromResource(): String {
        log.debug("Loading version information")
        val props = Properties()
        val propFileName = "project.properties"
        val inputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(propFileName)

        if (inputStream == null) {
            throw FileNotFoundException("property file '" + propFileName + "' not found in the classpath")
        }

        props.load(inputStream)

        val projectVersion = props["project.version"] as String
        log.debug("Found project version [$projectVersion]")
        return projectVersion
    }

    public abstract fun getPluginClassName():String

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
    public override fun getPluginClassName():String {
        return "org.jetbrains.kotlin.gradle.plugin.KotlinPlugin"
    }
}

open class KotlinAndriodPluginWrapper: KotlinBasePluginWrapper() {
    public override fun getPluginClassName():String {
        return "org.jetbrains.kotlin.gradle.plugin.KotlinAndroidPlugin"
    }
}

open class Kotlin2JsPluginWrapper : KotlinBasePluginWrapper() {
    public override fun getPluginClassName():String {
        return "org.jetbrains.kotlin.gradle.plugin.Kotlin2JsPlugin"
    }
}

