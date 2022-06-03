package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleDependency
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.topLevelExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.toModuleDependency
import org.jetbrains.kotlin.project.model.KotlinModuleDependency
import org.jetbrains.kotlin.project.model.LocalModuleIdentifier
import org.jetbrains.kotlin.project.model.MavenModuleIdentifier

internal data class VersionedMavenModuleIdentifier(val moduleId: MavenModuleIdentifier, val version: String)

private fun localModuleDependenciesToPublishedModuleMapping(
    project: Project,
    dependencies: Iterable<KotlinModuleDependency>
): Map<KotlinModuleDependency, VersionedMavenModuleIdentifier> {
    return dependencies.mapNotNull mapping@{ dependency ->
        val moduleIdentifier = dependency.moduleIdentifier
        val resolvesToProject =
            if (moduleIdentifier is LocalModuleIdentifier && moduleIdentifier.buildId == project.currentBuildId().name)
                project.project(moduleIdentifier.projectId)
            else
                return@mapping null

        val moduleClassifier = moduleIdentifier.moduleClassifier
        when (val ext = resolvesToProject.topLevelExtensionOrNull) {
            is KotlinPm20ProjectExtension -> {
                val module = ext.modules.find { it.moduleClassifier == moduleClassifier } ?: return@mapping null

                when (module.publicationMode) {
                    Private -> {
                        error("A dependency on $module can't be published because the module is not published.")
                    }

                    is Standalone, Embedded -> {
                        val coordinates = module.publicationHolder()?.publishedMavenModuleCoordinates
                            ?: return@mapping null

                        dependency to VersionedMavenModuleIdentifier(
                            MavenModuleIdentifier(
                                coordinates.group,
                                coordinates.name,
                                module.moduleClassifier.takeIf { module.publicationMode is Embedded }
                            ),
                            coordinates.version
                        )
                    }
                }
            }

            is KotlinMultiplatformExtension -> {
                val rootPublication = ext.rootSoftwareComponent.publicationDelegate
                val group = rootPublication?.groupId ?: project.group.toString()
                val name = rootPublication?.artifactId ?: project.name
                val version = rootPublication?.version ?: project.version.toString()
                dependency to VersionedMavenModuleIdentifier(MavenModuleIdentifier(group, name, null), version)
            }

            else -> null
        }
    }.toMap()
}

internal fun replaceProjectDependenciesWithPublishedMavenIdentifiers(
    project: Project,
    dependencies: Iterable<KotlinModuleDependency>
): Set<MavenModuleIdentifier> {
    val mapping = localModuleDependenciesToPublishedModuleMapping(project, dependencies)
    return dependencies.mapNotNull { dependency ->
        val replacement = mapping[dependency]
        val id = dependency.moduleIdentifier
        when {
            replacement != null -> replacement.moduleId

            id is MavenModuleIdentifier -> id

            id is LocalModuleIdentifier && id.buildId == project.currentBuildId().name -> {
                val otherProject = project.project(id.projectId)
                // TODO: find single publication with maven-publish in non-MPP projects?
                MavenModuleIdentifier(otherProject.group.toString(), otherProject.name, otherProject.version.toString())
            }

            else -> null
        }
    }.toSet()
}

internal fun replaceProjectDependenciesWithPublishedMavenDependencies(
    project: Project,
    dependencies: Iterable<Dependency>
): List<Dependency> {
    val dependencyToKotlinModuleDependency = dependencies.associateWith { it.toModuleDependency(project) }
    val mapping = localModuleDependenciesToPublishedModuleMapping(project, dependencyToKotlinModuleDependency.values)
    return dependencies.map { dependency ->
        val replacement = mapping[dependencyToKotlinModuleDependency.getValue(dependency)]
        if (replacement != null)
            project.dependencies.create("${replacement.moduleId.group}:${replacement.moduleId.name}:${replacement.version}").apply {
                if (replacement.moduleId.moduleClassifier != null) {
                    (this as ModuleDependency).capabilities {
                        it.requireCapability(checkNotNull(ComputedCapability.forAuxiliaryModuleByCoordinatesAndName(project, replacement)))
                    }
                }
            }
        else {
            dependency
        }
    }

}