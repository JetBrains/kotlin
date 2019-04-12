/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.targets.metadata.getPublishedPlatformCompilations
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

data class KotlinProjectStructureMetadata(
    @Input
    val sourceSetNamesByVariantName: Map<String, Set<String>>,

    @Input
    val sourceSetsDependsOnRelation: Map<String, Set<String>>,

    @Input
    val sourceSetModuleDependencies: Map<String, Set<Pair<String, String>>>
)

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
            sourceSet.name to project.configurations.getByName(sourceSet.apiConfigurationName).allDependencies.map {
                it.group.orEmpty() to it.name
            }.toSet()
        }
    )
}

internal fun KotlinProjectStructureMetadata.toXmlDocument(): Document {
    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().apply {
        fun Node.node(name: String, action: Element.() -> Unit) = appendChild(createElement(name).apply(action))
        fun Node.textNode(name: String, value: String) =
            appendChild(createElement(name).apply { appendChild(createTextNode(value)) })

        node("projectStructure") {
            node("variants") {
                sourceSetNamesByVariantName.forEach { (variantName, sourceSets) ->
                    node("variant") {
                        textNode("name", variantName)
                        sourceSets.forEach { sourceSetName -> textNode("sourceSet", sourceSetName) }
                    }
                }
            }

            node("sourceSets") {
                val keys = sourceSetsDependsOnRelation.keys + sourceSetModuleDependencies.keys
                for (sourceSet in keys) {
                    node("sourceSet") {
                        textNode("name", sourceSet)
                        sourceSetsDependsOnRelation[sourceSet].orEmpty().forEach { dependsOn ->
                            textNode("dependsOn", dependsOn)
                        }
                        sourceSetModuleDependencies[sourceSet].orEmpty().forEach { moduleDependency ->
                            textNode("moduleDependency", moduleDependency.first + ":" + moduleDependency.second)
                        }
                    }
                }
            }
        }
    }
    return document
}

private val NodeList.elements: Iterable<Element> get() = (0 until length).map { this@elements.item(it) }.filterIsInstance<Element>()

internal fun parseKotlinSourceSetMetadataFromXml(document: Document): KotlinProjectStructureMetadata? {
    val projectStructureNode = document.getElementsByTagName("projectStructure").elements.single()

    val variantsNode = projectStructureNode.getElementsByTagName("variants").item(0) ?: return null

    val sourceSetsByVariant = mutableMapOf<String, Set<String>>()

    variantsNode.childNodes.elements.filter { it.tagName == "variant" }.forEach { variantNode ->
        val variantName = variantNode.getElementsByTagName("name").elements.single().textContent
        val sourceSets = variantNode.childNodes.elements.filter { it.tagName == "sourceSet" }.mapTo(mutableSetOf()) { it.textContent }

        sourceSetsByVariant[variantName] = sourceSets
    }

    val sourceSetDependsOnRelation = mutableMapOf<String, Set<String>>()
    val sourceSetModuleDependencies = mutableMapOf<String, Set<Pair<String, String>>>()

    val sourceSetsNode = projectStructureNode.getElementsByTagName("sourceSets").item(0) ?: return null

    sourceSetsNode.childNodes.elements.filter { it.tagName == "sourceSet" }.forEach { sourceSetNode ->
        val sourceSetName = sourceSetNode.getElementsByTagName("name").elements.single().textContent

        val dependsOn = mutableSetOf<String>()
        val moduleDependencies = mutableSetOf<Pair<String, String>>()

        sourceSetNode.childNodes.elements.forEach {
            when (it.tagName) {
                "dependsOn" -> dependsOn.add(it.textContent)
                "moduleDependency" -> moduleDependencies.add(it.textContent.split(":").let { (first, second) -> first to second })
            }
        }

        sourceSetDependsOnRelation[sourceSetName] = dependsOn
        sourceSetModuleDependencies[sourceSetName] = moduleDependencies
    }

    return KotlinProjectStructureMetadata(
        sourceSetsByVariant,
        sourceSetDependsOnRelation,
        sourceSetModuleDependencies
    )
}