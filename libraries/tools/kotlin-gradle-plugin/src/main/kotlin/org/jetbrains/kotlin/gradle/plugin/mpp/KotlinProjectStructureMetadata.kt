/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.getSourceSetHierarchy
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.metadata.getPublishedPlatformCompilations
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

data class ModuleDependencyIdentifier(
    val groupId: String?,
    val moduleId: String
)

sealed class SourceSetMetadataLayout(
    @get:Input
    val name: String,
    @get:Internal
    val archiveExtension: String
) {
    object METADATA : SourceSetMetadataLayout("metadata", "jar")
    object KLIB : SourceSetMetadataLayout("klib", "klib")

    companion object {
        private val values = listOf(METADATA, KLIB)

        fun byName(name: String): SourceSetMetadataLayout? = values.firstOrNull { it.name == name }

        fun chooseForProducingProject(project: Project) =
            /** A producing project will now only generate Granular source sets metadata as a KLIB */
            KLIB
    }
}

data class KotlinProjectStructureMetadata(
    @Input
    val sourceSetNamesByVariantName: Map<String, Set<String>>,

    @Input
    val sourceSetsDependsOnRelation: Map<String, Set<String>>,

    @Nested
    val sourceSetBinaryLayout: Map<String, SourceSetMetadataLayout>,

    @Internal
    val sourceSetModuleDependencies: Map<String, Set<ModuleDependencyIdentifier>>,

    @Input
    val hostSpecificSourceSets: Set<String>,

    @Input
    val formatVersion: String = FORMAT_VERSION_0_3
) {
    @Suppress("UNUSED") // Gradle input
    @get:Input
    internal val sourceSetModuleDependenciesInput: Map<String, Set<Pair<String, String>>>
        get() = sourceSetModuleDependencies.mapValues { (_, ids) -> ids.map { (group, module) -> group.orEmpty() to module }.toSet() }

    companion object {
        internal const val FORMAT_VERSION_0_1 = "0.1"

        // + binaryFormat (klib, metadata/jar)
        internal const val FORMAT_VERSION_0_2 = "0.2"

        // + 'hostSpecific' flag for source sets
        internal const val FORMAT_VERSION_0_3 = "0.3"
    }
}

internal fun buildKotlinProjectStructureMetadata(project: Project): KotlinProjectStructureMetadata? {
    val sourceSetsWithMetadataCompilations =
        project.multiplatformExtensionOrNull?.targets?.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME)?.compilations?.associate {
            it.defaultSourceSet to it
        } ?: return null

    val publishedVariantsNamesWithCompilation = getPublishedPlatformCompilations(project).mapKeys { it.key.name }

    return KotlinProjectStructureMetadata(
        sourceSetNamesByVariantName = publishedVariantsNamesWithCompilation.mapValues { (_, compilation) ->
            compilation.allKotlinSourceSets.filter { it in sourceSetsWithMetadataCompilations }.map { it.name }.toSet()
        },
        sourceSetsDependsOnRelation = sourceSetsWithMetadataCompilations.keys.associate { sourceSet ->
            sourceSet.name to sourceSet.dependsOn.filter { it in sourceSetsWithMetadataCompilations }.map { it.name }.toSet()
        },
        sourceSetModuleDependencies = sourceSetsWithMetadataCompilations.keys.associate { sourceSet ->
            /**
             * Currently, Kotlin/Native dependencies must include the implementation dependencies, too. These dependencies must also be
             * published as API dependencies of the metadata module to get into the resolution result, see
             * [KotlinMetadataTargetConfigurator.exportDependenciesForPublishing].
             */
            val isNativeSharedSourceSet = sourceSetsWithMetadataCompilations[sourceSet] is KotlinSharedNativeCompilation
            val sourceSetExportedDependencies = when {
                isNativeSharedSourceSet -> sourceSet.getSourceSetHierarchy().flatMap { hierarchySourceSet ->
                    listOf(KotlinDependencyScope.API_SCOPE, KotlinDependencyScope.IMPLEMENTATION_SCOPE).flatMap { scope ->
                        project.sourceSetDependencyConfigurationByScope(hierarchySourceSet, scope).allDependencies.toList()
                    }
                }.distinct()
                else -> project.configurations.getByName(sourceSet.apiConfigurationName).allDependencies
            }
            sourceSet.name to sourceSetExportedDependencies.map { ModuleIds.fromDependency(it) }.toSet()
        },
        hostSpecificSourceSets = getHostSpecificSourceSets(project).map { it.name }.toSet(),
        sourceSetBinaryLayout = sourceSetsWithMetadataCompilations.keys.associate { sourceSet ->
            sourceSet.name to SourceSetMetadataLayout.chooseForProducingProject(project)
        }
    )
}

internal fun KotlinProjectStructureMetadata.toXmlDocument(): Document {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().apply {
        fun Node.node(name: String, action: Element.() -> Unit) = appendChild(createElement(name).apply(action))
        fun Node.textNode(name: String, value: String) =
            appendChild(createElement(name).apply { appendChild(createTextNode(value)) })

        node(ROOT_NODE_NAME) {
            textNode(FORMAT_VERSION_NODE_NAME, formatVersion)

            node(VARIANTS_NODE_NAME) {
                sourceSetNamesByVariantName.forEach { (variantName, sourceSets) ->
                    node(VARIANT_NODE_NAME) {
                        textNode(NAME_NODE_NAME, variantName)
                        sourceSets.forEach { sourceSetName -> textNode(SOURCE_SET_NODE_NAME, sourceSetName) }
                    }
                }
            }

            node(SOURCE_SETS_NODE_NAME) {
                val keys = sourceSetsDependsOnRelation.keys + sourceSetModuleDependencies.keys
                for (sourceSet in keys) {
                    node(SOURCE_SET_NODE_NAME) {
                        textNode(NAME_NODE_NAME, sourceSet)
                        sourceSetsDependsOnRelation[sourceSet].orEmpty().forEach { dependsOn ->
                            textNode(DEPENDS_ON_NODE_NAME, dependsOn)
                        }
                        sourceSetModuleDependencies[sourceSet].orEmpty().forEach { moduleDependency ->
                            textNode(MODULE_DEPENDENCY_NODE_NAME, moduleDependency.groupId + ":" + moduleDependency.moduleId)
                        }
                        sourceSetBinaryLayout[sourceSet]?.let { binaryLayout ->
                            textNode(BINARY_LAYOUT_NODE_NAME, binaryLayout.name)
                        }
                        if (sourceSet in hostSpecificSourceSets) {
                            textNode(HOST_SPECIFIC_NODE_NAME, "true")
                        }
                    }
                }
            }
        }
    }
}

private val NodeList.elements: Iterable<Element> get() = (0 until length).map { this@elements.item(it) }.filterIsInstance<Element>()

internal fun parseKotlinSourceSetMetadataFromXml(document: Document): KotlinProjectStructureMetadata? {
    val projectStructureNode = document.getElementsByTagName(ROOT_NODE_NAME).elements.single()

    val formatVersion = projectStructureNode.getElementsByTagName(FORMAT_VERSION_NODE_NAME).item(0).textContent

    val variantsNode = projectStructureNode.getElementsByTagName(VARIANTS_NODE_NAME).item(0) ?: return null

    val sourceSetsByVariant = mutableMapOf<String, Set<String>>()

    variantsNode.childNodes.elements.filter { it.tagName == VARIANT_NODE_NAME }.forEach { variantNode ->
        val variantName = variantNode.getElementsByTagName(NAME_NODE_NAME).elements.single().textContent
        val sourceSets =
            variantNode.childNodes.elements.filter { it.tagName == SOURCE_SET_NODE_NAME }.mapTo(mutableSetOf()) { it.textContent }

        sourceSetsByVariant[variantName] = sourceSets
    }

    val sourceSetDependsOnRelation = mutableMapOf<String, Set<String>>()
    val sourceSetModuleDependencies = mutableMapOf<String, Set<ModuleDependencyIdentifier>>()
    val sourceSetBinaryLayout = mutableMapOf<String, SourceSetMetadataLayout>()
    val hostSpecificSourceSets = mutableSetOf<String>()

    val sourceSetsNode = projectStructureNode.getElementsByTagName(SOURCE_SETS_NODE_NAME).item(0) ?: return null

    sourceSetsNode.childNodes.elements.filter { it.tagName == SOURCE_SET_NODE_NAME }.forEach { sourceSetNode ->
        val sourceSetName = sourceSetNode.getElementsByTagName(NAME_NODE_NAME).elements.single().textContent

        val dependsOn = mutableSetOf<String>()
        val moduleDependencies = mutableSetOf<ModuleDependencyIdentifier>()

        sourceSetNode.childNodes.elements.forEach { node ->
            when (node.tagName) {
                DEPENDS_ON_NODE_NAME -> dependsOn.add(node.textContent)
                MODULE_DEPENDENCY_NODE_NAME -> {
                    val (groupId, moduleId) = node.textContent.split(":")
                    moduleDependencies.add(ModuleDependencyIdentifier(groupId, moduleId))
                }
                BINARY_LAYOUT_NODE_NAME -> {
                    SourceSetMetadataLayout.byName(node.textContent)?.let { binaryLayout ->
                        sourceSetBinaryLayout[sourceSetName] = binaryLayout
                    }
                }
                HOST_SPECIFIC_NODE_NAME -> {
                    if (node.textContent == "true") {
                        hostSpecificSourceSets.add(sourceSetName)
                    }
                }
            }
        }

        sourceSetDependsOnRelation[sourceSetName] = dependsOn
        sourceSetModuleDependencies[sourceSetName] = moduleDependencies
    }

    return KotlinProjectStructureMetadata(
        sourceSetsByVariant,
        sourceSetDependsOnRelation,
        sourceSetBinaryLayout,
        sourceSetModuleDependencies,
        hostSpecificSourceSets,
        formatVersion
    )
}

private const val ROOT_NODE_NAME = "projectStructure"
private const val FORMAT_VERSION_NODE_NAME = "formatVersion"
private const val VARIANTS_NODE_NAME = "variants"
private const val VARIANT_NODE_NAME = "variant"
private const val NAME_NODE_NAME = "name"
private const val SOURCE_SETS_NODE_NAME = "sourceSets"
private const val SOURCE_SET_NODE_NAME = "sourceSet"
private const val DEPENDS_ON_NODE_NAME = "dependsOn"
private const val MODULE_DEPENDENCY_NODE_NAME = "moduleDependency"
private const val BINARY_LAYOUT_NODE_NAME = "binaryLayout"
private const val HOST_SPECIFIC_NODE_NAME = "hostSpecific"