/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.publishing

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.GenerateMavenPom

internal data class ModuleCoordinates(
    private val moduleGroup: String?,
    private val moduleName: String,
    private val moduleVersion: String?,
) : ModuleVersionIdentifier {
    override fun getGroup() = moduleGroup ?: "unspecified"
    override fun getName() = moduleName
    override fun getVersion() = moduleVersion ?: "unspecified"

    override fun getModule(): ModuleIdentifier = object : ModuleIdentifier {
        override fun getGroup(): String = moduleGroup ?: "unspecified"
        override fun getName(): String = moduleName
    }
}

// FIXME: 28.11.2024 - Наверное смысл PomDependenciesRewriter'а в том чтобы переписывать зависимости на платформенные слайсы
internal abstract class PomDependenciesRewriter {

    open val taskDependencies: Any? get() = null

    abstract fun createDependenciesMappingForEachUsageContext(): List<Map<ModuleCoordinates, ModuleCoordinates>>

    fun rewritePomMppDependenciesToActualTargetModules(
        pomXml: XmlProvider,
        includeOnlySpecifiedDependencies: Provider<Set<ModuleCoordinates>>? = null,
    ) {
        val dependenciesNode = (pomXml.asNode().get("dependencies") as NodeList).filterIsInstance<Node>().singleOrNull() ?: return

        val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()

        val dependencyByNode = mutableMapOf<Node, ModuleCoordinates>()

        // Collect all the dependencies from the nodes:
        val dependencies = dependencyNodes.map { dependencyNode ->
            fun Node.getSingleChildValueOrNull(childName: String): String? =
                ((get(childName) as NodeList?)?.singleOrNull() as Node?)?.text()

            val groupId = dependencyNode.getSingleChildValueOrNull("groupId")
            val artifactId = dependencyNode.getSingleChildValueOrNull("artifactId")
                ?: error("unexpected dependency in POM with no artifact ID: $dependenciesNode")
            val version = dependencyNode.getSingleChildValueOrNull("version")
            (ModuleCoordinates(groupId, artifactId, version)).also { dependencyByNode[dependencyNode] = it }
        }.toSet()

        val dependenciesMappingForEachUsageContext = createDependenciesMappingForEachUsageContext()
        val resultDependenciesForEachUsageContext = dependencies.associateWith { key ->
            val map = dependenciesMappingForEachUsageContext.find { key in it }
            val value = map?.get(key) ?: key
            value
        }

        val includeOnlySpecifiedDependenciesSet = includeOnlySpecifiedDependencies?.get()

        // Rewrite the dependency nodes according to the mapping:
        dependencyNodes.forEach { dependencyNode ->
            val moduleDependency = dependencyByNode[dependencyNode]

            if (moduleDependency != null) {
                if (includeOnlySpecifiedDependenciesSet != null && moduleDependency !in includeOnlySpecifiedDependenciesSet) {
                    dependenciesNode.remove(dependencyNode)
                    return@forEach
                }
            }

            val mapDependencyTo = resultDependenciesForEachUsageContext.get(moduleDependency)

            if (mapDependencyTo != null) {
                fun Node.setChildNodeByName(name: String, value: String?) {
                    val childNode: Node? = (get(name) as NodeList?)?.firstOrNull() as Node?
                    if (value != null) {
                        (childNode ?: appendNode(name)).setValue(value)
                    } else {
                        childNode?.let { remove(it) }
                    }
                }

                dependencyNode.setChildNodeByName("groupId", mapDependencyTo.group)
                dependencyNode.setChildNodeByName("artifactId", mapDependencyTo.name)
                dependencyNode.setChildNodeByName("version", mapDependencyTo.version)
            }
        }
    }
}

internal fun Project.addTaskDependenciesToPomGenerator(publication: MavenPublication, dependencies: Any) {
    val pom = publication.pom
    tasks.withType(GenerateMavenPom::class.java).configureEach {
        if (it.pom === pom) {
            it.dependsOn(dependencies)
        }
    }
}