package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.specs.Spec
import java.io.File
import java.net.URL
import org.gradle.api.logging.Logging
import java.util.Properties
import java.io.FileNotFoundException

abstract class KotlinBasePluginWrapper: Plugin<Project> {

    val log = Logging.getLogger(getClass())!!

    public override fun apply(project: Project) {
        val dependencyHandler : DependencyHandler = project.getBuildscript().getDependencies()
        val configurationsContainer : ConfigurationContainer = project.getBuildscript().getConfigurations()

        log.debug("Loading version information")
        val props = Properties()
        val propFileName = "project.properties"
        val inputStream = getClass().getClassLoader()!!.getResourceAsStream(propFileName)

        if (inputStream == null) {
            throw FileNotFoundException("property file '" + propFileName + "' not found in the classpath")
        }

        props.load(inputStream);

        val projectVersion = props.get("project.version") as String
        log.debug("Found project version [$projectVersion]")
        project.getExtensions().getExtraProperties()?.set("kotlin.gradle.plugin.version", projectVersion)

        log.debug("Creating configuration and dependency")
        val kotlinPluginCoreCoordinates = "org.jetbrains.kotlin:kotlin-gradle-plugin-core:" + projectVersion
        val dependency = dependencyHandler.create(kotlinPluginCoreCoordinates)
        val configuration = configurationsContainer.detachedConfiguration(dependency)!!

        log.debug("Resolving [" + kotlinPluginCoreCoordinates + "]")
        val kotlinPluginDependencies : List<URL> = configuration.getResolvedConfiguration().getFiles(KSpec({ dep -> true }))!!.map({(f: File):URL -> f.toURI().toURL() })
        log.debug("Resolved files: [" + kotlinPluginDependencies.toString() + "]")
        log.debug("Load plugin in parent-last URL classloader")
        val kotlinPluginClassloader = ParentLastURLClassLoader(kotlinPluginDependencies, getClass().getClassLoader())
        log.debug("Class loader created")
        val cls = Class.forName(getPluginClassName(), true, kotlinPluginClassloader)
        log.debug("Plugin class loaded")
        val pluginInstance = cls.newInstance()
        log.debug("Plugin class instantiated")

        val applyMethod = cls.getMethod("apply", javaClass<Project>())
        log.debug("'apply' method found, invoking...")
        applyMethod.invoke(pluginInstance, project);
        log.debug("'apply' method invoked successfully")
    }

    public abstract fun getPluginClassName():String
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
