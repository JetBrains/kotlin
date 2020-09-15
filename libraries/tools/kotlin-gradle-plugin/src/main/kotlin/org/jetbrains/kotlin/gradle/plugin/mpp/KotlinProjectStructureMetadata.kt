/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
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
import java.io.StringWriter
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

    override fun toString(): String = name

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

internal fun <Serializer> KotlinProjectStructureMetadata.serialize(
    serializer: Serializer,
    node: Serializer.(name: String, Serializer.() -> Unit) -> Unit,
    multiNodes: Serializer.(name: String, Serializer.() -> Unit) -> Unit,
    multiNodesItem: Serializer.(name: String, Serializer.() -> Unit) -> Unit,
    value: Serializer.(key: String, value: String) -> Unit,
    multiValue: Serializer.(name: String, values: List<String>) -> Unit
) = with(serializer) {
    node(ROOT_NODE_NAME) {
        value(FORMAT_VERSION_NODE_NAME, formatVersion)

        multiNodes(VARIANTS_NODE_NAME) {
            sourceSetNamesByVariantName.forEach { (variantName, sourceSets) ->
                multiNodesItem(VARIANT_NODE_NAME) {
                    value(NAME_NODE_NAME, variantName)
                    multiValue(SOURCE_SET_NODE_NAME, sourceSets.toList())
                }
            }
        }

        multiNodes(SOURCE_SETS_NODE_NAME) {
            val keys = sourceSetsDependsOnRelation.keys + sourceSetModuleDependencies.keys
            for (sourceSet in keys) {
                multiNodesItem(SOURCE_SET_NODE_NAME) {
                    value(NAME_NODE_NAME, sourceSet)
                    multiValue(DEPENDS_ON_NODE_NAME, sourceSetsDependsOnRelation[sourceSet].orEmpty().toList())
                    multiValue(MODULE_DEPENDENCY_NODE_NAME, sourceSetModuleDependencies[sourceSet].orEmpty().map { moduleDependency ->
                        moduleDependency.groupId + ":" + moduleDependency.moduleId
                    })
                    sourceSetBinaryLayout[sourceSet]?.let { binaryLayout ->
                        value(BINARY_LAYOUT_NODE_NAME, binaryLayout.name)
                    }
                    if (sourceSet in hostSpecificSourceSets) {
                        value(HOST_SPECIFIC_NODE_NAME, "true")
                    }
                }
            }
        }
    }
}

internal fun KotlinProjectStructureMetadata.toXmlDocument(): Document {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().apply {
        val node: Node.(String, Node.() -> Unit) -> Unit = { name, content -> appendChild(createElement(name).apply(content)) }
        val textNode: Node.(String, String) -> Unit =
            { name, value -> appendChild(createElement(name).apply { appendChild(createTextNode(value)) }) }
        serialize(this as Node, node, node, node, textNode, { name, values -> for (v in values) textNode(name, v) })
    }
}

internal fun KotlinProjectStructureMetadata.toJson(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val stringWriter = StringWriter()
    with(gson.newJsonWriter(stringWriter)) {
        val obj: JsonWriter.(String, JsonWriter.() -> Unit) -> Unit =
            { name, content -> if (name.isNotEmpty()) name(name); beginObject(); content(); endObject() }
        val property: JsonWriter.(String, String) -> Unit = { name, value -> name(name); value(value) }
        val array: JsonWriter.(String, JsonWriter.() -> Unit) -> Unit =
            { name, contents -> name(name); beginArray(); contents(); endArray() }

        beginObject()
        serialize(this, obj, array, { _, fn -> obj("", fn) }, property, { key, values -> array(key) { values.forEach { value(it) } } })
        endObject()
    }
    return stringWriter.toString()
}

private val NodeList.elements: Iterable<Element> get() = (0 until length).map { this@elements.item(it) }.filterIsInstance<Element>()

internal fun parseKotlinSourceSetMetadataFromJson(string: String): KotlinProjectStructureMetadata? {
    @Suppress("DEPRECATION") // The replacement doesn't compile against old dependencies such as AS 4.0
    val json = JsonParser().parse(string).asJsonObject
    val nodeNamed: JsonObject.(String) -> JsonObject? = { name -> get(name)?.asJsonObject }
    val valueNamed: JsonObject.(String) -> String? = { name -> get(name)?.asString }
    val multiObjects: JsonObject.(String?) -> Iterable<JsonObject> = { name -> get(name).asJsonArray.map { it.asJsonObject } }
    val multiValues: JsonObject.(String?) -> Iterable<String> = { name -> get(name).asJsonArray.map { it.asString } }

    return parseKotlinSourceSetMetadata({ json.get(ROOT_NODE_NAME).asJsonObject }, valueNamed, multiObjects, multiValues)
}

internal fun parseKotlinSourceSetMetadataFromXml(document: Document): KotlinProjectStructureMetadata? {
    val nodeNamed: Element.(String) -> Element? = { name -> getElementsByTagName(name).elements.singleOrNull() }
    val valueNamed: Element.(String) -> String? =
        { name -> getElementsByTagName(name).run { if (length > 0) item(0).textContent else null } }
    val multiObjects: Element.(String) -> Iterable<Element> = { name -> nodeNamed(name)?.childNodes?.elements ?: emptyList()}
    val multiValues: Element.(String) -> Iterable<String> = { name -> getElementsByTagName(name).elements.map { it.textContent } }

    return parseKotlinSourceSetMetadata(
        { document.getElementsByTagName(ROOT_NODE_NAME).elements.single() },
        valueNamed,
        multiObjects,
        multiValues
    )
}

internal fun <ParsingContext> parseKotlinSourceSetMetadata(
    getRoot: () -> ParsingContext,
    valueNamed: ParsingContext.(key: String) -> String?,
    multiObjects: ParsingContext.(named: String) -> Iterable<ParsingContext>,
    multiValues: ParsingContext.(named: String) -> Iterable<String>
): KotlinProjectStructureMetadata? {
    val projectStructureNode = getRoot()

    val formatVersion = checkNotNull(projectStructureNode.valueNamed(FORMAT_VERSION_NODE_NAME))
    val variantsNode = projectStructureNode.multiObjects(VARIANTS_NODE_NAME)

    val sourceSetsByVariant = mutableMapOf<String, Set<String>>()

    variantsNode.forEach { variantNode ->
        val variantName = requireNotNull(variantNode.valueNamed(NAME_NODE_NAME))
        val sourceSets = variantNode.multiValues(SOURCE_SET_NODE_NAME).toSet()

        sourceSetsByVariant[variantName] = sourceSets
    }

    val sourceSetDependsOnRelation = mutableMapOf<String, Set<String>>()
    val sourceSetModuleDependencies = mutableMapOf<String, Set<ModuleDependencyIdentifier>>()
    val sourceSetBinaryLayout = mutableMapOf<String, SourceSetMetadataLayout>()
    val hostSpecificSourceSets = mutableSetOf<String>()

    val sourceSetsNode = projectStructureNode.multiObjects(SOURCE_SETS_NODE_NAME)

    sourceSetsNode.forEach { sourceSetNode ->
        val sourceSetName = checkNotNull(sourceSetNode.valueNamed(NAME_NODE_NAME))

        val dependsOn = sourceSetNode.multiValues(DEPENDS_ON_NODE_NAME).toSet()
        val moduleDependencies = sourceSetNode.multiValues(MODULE_DEPENDENCY_NODE_NAME).mapTo(mutableSetOf()) {
            val (groupId, moduleId) = it.split(":")
            ModuleDependencyIdentifier(groupId, moduleId)
        }

        sourceSetNode.valueNamed(HOST_SPECIFIC_NODE_NAME)
            ?.let { if (it.toBoolean()) hostSpecificSourceSets.add(sourceSetName) }

        sourceSetNode.valueNamed(BINARY_LAYOUT_NODE_NAME)
            ?.let { SourceSetMetadataLayout.byName(it) }
            ?.let { sourceSetBinaryLayout[sourceSetName] = it }

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