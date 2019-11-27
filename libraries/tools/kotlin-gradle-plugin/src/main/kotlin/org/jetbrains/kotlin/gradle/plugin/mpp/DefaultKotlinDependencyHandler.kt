package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency

class DefaultKotlinDependencyHandler(
    val parent: HasKotlinDependencies,
    val project: Project
) : KotlinDependencyHandler {
    override fun api(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.apiConfigurationName, dependencyNotation)

    override fun api(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.apiConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> api(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.apiConfigurationName, dependency, configure)

    override fun implementation(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.implementationConfigurationName, dependencyNotation)

    override fun implementation(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.implementationConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> implementation(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.implementationConfigurationName, dependency, configure)

    override fun compileOnly(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.compileOnlyConfigurationName, dependencyNotation)

    override fun compileOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.compileOnlyConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> compileOnly(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.compileOnlyConfigurationName, dependency, configure)

    override fun runtimeOnly(dependencyNotation: Any): Dependency? =
        addDependencyByAnyNotation(parent.runtimeOnlyConfigurationName, dependencyNotation)

    override fun runtimeOnly(dependencyNotation: String, configure: ExternalModuleDependency.() -> Unit): ExternalModuleDependency =
        addDependencyByStringNotation(parent.runtimeOnlyConfigurationName, dependencyNotation, configure)

    override fun <T : Dependency> runtimeOnly(dependency: T, configure: T.() -> Unit): T =
        addDependency(parent.runtimeOnlyConfigurationName, dependency, configure)

    override fun kotlin(simpleModuleName: String, version: String?): ExternalModuleDependency =
        project.dependencies.create(
            "org.jetbrains.kotlin:kotlin-$simpleModuleName" + version?.let { ":$it" }.orEmpty()
        ) as ExternalModuleDependency

    override fun project(notation: Map<String, Any?>): ProjectDependency =
        project.dependencies.project(notation) as ProjectDependency

    private fun addDependencyByAnyNotation(
        configurationName: String,
        dependencyNotation: Any
    ): Dependency? =
        project.dependencies.add(configurationName, dependencyNotation)

    private fun addDependencyByStringNotation(
        configurationName: String,
        dependencyNotation: Any,
        configure: ExternalModuleDependency.() -> Unit = { }
    ): ExternalModuleDependency =
        addDependency(configurationName, project.dependencies.create(dependencyNotation) as ExternalModuleDependency, configure)

    private fun <T : Dependency> addDependency(
        configurationName: String,
        dependency: T,
        configure: T.() -> Unit
    ): T =
        dependency.also {
            configure(it)
            project.dependencies.add(configurationName, it)
        }

    override fun npm(name: String, version: String): NpmDependency =
        NpmDependency(project, name, version)

    override fun npm(org: String?, packageName: String, version: String) =
        npm("${if (org != null) "@$org/" else ""}$packageName", version)
}
