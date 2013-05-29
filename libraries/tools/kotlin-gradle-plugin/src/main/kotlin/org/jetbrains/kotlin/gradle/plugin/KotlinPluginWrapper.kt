package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.specs.Spec
import java.io.File
import java.net.URL

open class KotlinPluginWrapper: Plugin<Project> {
    public override fun apply(project: Project) {
        val dependencyHandler : DependencyHandler = project.getDependencies()!!
        val configurationsContainer : ConfigurationContainer = project.getConfigurations()!!

        val dependency = dependencyHandler.create("org.jetbrains.kotlin:kotlin-gradle-plugin-core:0.1-SNAPSHOT")
        val configuration = configurationsContainer.detachedConfiguration(dependency)!!

        val kotlinPluginDependencies : List<URL> = configuration.getResolvedConfiguration()!!.getFiles(KSpec({ dep -> true }))!!.map({(f: File):URL -> f.toURI().toURL() })
        val kotlinPluginClassloader = ParentLastURLClassLoader(kotlinPluginDependencies, getClass().getClassLoader())
        val cls = Class.forName("org.jetbrains.kotlin.gradle.plugin.KotlinPlugin", true, kotlinPluginClassloader)
        val pluginInstance = cls.newInstance()
        val applyMethod = cls.getMethod("apply", javaClass<Project>())

        applyMethod.invoke(pluginInstance, project);
    }
}

open class KSpec<T: Any?>(val predicate: (T) -> Boolean): Spec<T> {
    public override fun isSatisfiedBy(p0: T?): Boolean {
        return p0 != null && predicate(p0)
    }
}
