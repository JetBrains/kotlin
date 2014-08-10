package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.specs.Spec
import java.io.File
import java.net.URL
import org.gradle.api.logging.Logging
import java.util.Properties
import java.io.FileNotFoundException
import org.gradle.api.initialization.dsl.ScriptHandler

abstract class KotlinBasePluginWrapper: Plugin<Project> {

    val log = Logging.getLogger(this.javaClass)

    public override fun apply(project: Project) {
        val sourceBuildScript = findSourceBuildScript(project);
        if (sourceBuildScript == null) {
            log.error("Failed to determine source cofiguration of kotlin plugin. Can not download core. Please verify that this or any parent project " +
                      "contains 'kotlin-gradle-plugin' in buildscript's classpath configuration.");
            return;
        }
        val dependencyHandler : DependencyHandler = sourceBuildScript.getDependencies()
        val configurationsContainer : ConfigurationContainer = sourceBuildScript.getConfigurations()

        log.debug("Loading version information")
        val props = Properties()
        val propFileName = "project.properties"
        val inputStream = this.javaClass.getClassLoader()!!.getResourceAsStream(propFileName)

        if (inputStream == null) {
            throw FileNotFoundException("property file '" + propFileName + "' not found in the classpath")
        }

        props.load(inputStream);

        val projectVersion = props["project.version"] as String
        log.debug("Found project version [$projectVersion]")
        project.getExtensions().getExtraProperties()?.set("kotlin.gradle.plugin.version", projectVersion)

        log.debug("Creating configuration and dependency")
        val kotlinPluginCoreCoordinates = "org.jetbrains.kotlin:kotlin-gradle-plugin-core:" + projectVersion
        val dependency = dependencyHandler.create(kotlinPluginCoreCoordinates)
        val configuration = configurationsContainer.detachedConfiguration(dependency)

        log.debug("Resolving [" + kotlinPluginCoreCoordinates + "]")
        val kotlinPluginDependencies : List<URL> = configuration.getResolvedConfiguration().getFiles(KSpec({ dep -> true }))!!.map({(f: File):URL -> f.toURI().toURL() })
        log.debug("Resolved files: [" + kotlinPluginDependencies.toString() + "]")
        log.debug("Load plugin in parent-last URL classloader")
        val kotlinPluginClassloader = ParentLastURLClassLoader(kotlinPluginDependencies, this.javaClass.getClassLoader())
        log.debug("Class loader created")
        val cls = Class.forName(getPluginClassName(), true, kotlinPluginClassloader)
        log.debug("Plugin class loaded")
        val constructor = cls.getConstructor(javaClass<ScriptHandler>())
        val pluginInstance = constructor.newInstance(sourceBuildScript)
        log.debug("Plugin class instantiated")

        val applyMethod = cls.getMethod("apply", javaClass<Project>())
        log.debug("'apply' method found, invoking...")
        applyMethod.invoke(pluginInstance, project);
        log.debug("'apply' method invoked successfully")
    }

    public abstract fun getPluginClassName():String

    private fun findSourceBuildScript(project: Project): ScriptHandler? {
        log.debug("Looking for proper script handler")
        var curProject = project
        while (curProject != curProject.getParent()) {
            log.debug("Looking in project $project")
            val scriptHandler = curProject.getBuildscript()
            val found = scriptHandler.getConfigurations().findByName("classpath")?.find { it.name.contains("kotlin-gradle-plugin") } != null;
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

open class KSpec<T: Any?>(val predicate: (T) -> Boolean): Spec<T> {
    public override fun isSatisfiedBy(p0: T?): Boolean {
        return p0 != null && predicate(p0)
    }
}
