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
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmModule
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.metadata.dependsOnClosureWithInterCompilationDependencies
import org.jetbrains.kotlin.gradle.targets.metadata.getPublishedPlatformCompilations
import org.jetbrains.kotlin.gradle.targets.metadata.isNativeSourceSet
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropCommonizerCompositeMetadataJarBundling.cinteropMetadataDirectoryPath
import org.jetbrains.kotlin.gradle.utils.compositeBuildRootProject
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.getOrPut
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.Serializable
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory

// FIXME support module classifiers for PM2.0 or drop this class in favor of KotlinModuleIdentifier
open class ModuleDependencyIdentifier(
    open val groupId: String?,
    open val moduleId: String
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModuleDependencyIdentifier) return false

        if (groupId != other.groupId) return false
        if (moduleId != other.moduleId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId?.hashCode() ?: 0
        result = 31 * result + moduleId.hashCode()
        return result
    }

    operator fun component1(): String? = groupId
    operator fun component2(): String = moduleId

    override fun toString(): String {
        return "${groupId}-${moduleId}"
    }
}

class ChangingModuleDependencyIdentifier(
    val groupIdProvider: () -> String?,
    val moduleIdProvider: () -> String
) : ModuleDependencyIdentifier(groupIdProvider(), moduleIdProvider()) {
    override val groupId: String?
        get() = groupIdProvider()
    override val moduleId: String
        get() = moduleIdProvider()
}

sealed class SourceSetMetadataLayout(
    @get:Input
    val name: String,
    @get:Internal
    val archiveExtension: String
) : Serializable {
    object METADATA : SourceSetMetadataLayout("metadata", "jar")
    object KLIB : SourceSetMetadataLayout("klib", "klib")

    override fun toString(): String = name

    companion object {
        private val values get() = listOf(METADATA, KLIB)

        fun byName(name: String): SourceSetMetadataLayout? = values.firstOrNull { it.name == name }

        fun chooseForProducingProject() =
            /** A producing project will now only generate Granular source sets metadata as a KLIB */
            KLIB
    }
}

/**
 * Represents the structure of "shared" source sets within a Kotlin Project.
 * Note: This entity is designed to only list "shared" source sets.
 * No "platform" source sets will be listed.
 */
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
    val sourceSetCInteropMetadataDirectory: Map<String, String>,

    @Input
    val hostSpecificSourceSets: Set<String>,

    @get:Input
    val isPublishedAsRoot: Boolean,

    @get:Input
    val sourceSetNames: Set<String>,

    @Input
    val formatVersion: String = FORMAT_VERSION_0_3_3
) : Serializable {
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

        // + 'isPublishedInRootModule' top-level flag
        internal const val FORMAT_VERSION_0_3_1 = "0.3.1"

        // + 'sourceSetCInteropMetadataDirectory' map
        internal const val FORMAT_VERSION_0_3_2 = "0.3.2"

        // + 'sourceSetsNames'
        internal const val FORMAT_VERSION_0_3_3 = "0.3.3"
    }
}


internal val KotlinMultiplatformExtension.kotlinProjectStructureMetadata: KotlinProjectStructureMetadata
    get() = project.extensions.extraProperties.getOrPut("org.jetbrains.kotlin.gradle.plugin.mpp.kotlinProjectStructureMetadata") {
        buildKotlinProjectStructureMetadata(this)
    }

private fun buildKotlinProjectStructureMetadata(extension: KotlinMultiplatformExtension): KotlinProjectStructureMetadata {
    val project = extension.project
    require(project.state.executed) { "Cannot build 'KotlinProjectStructureMetadata' during project configuration phase" }

    val sourceSetsWithMetadataCompilations = extension.targets
        .getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME)
        .compilations.associateBy { it.defaultSourceSet }

    val publishedVariantsNamesWithCompilation = project.future { getPublishedPlatformCompilations(project).mapKeys { it.key.name } }
        .getOrThrow()

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
            val isNativeSharedSourceSet = sourceSet.isNativeSourceSet.getOrThrow()
            val scopes = listOfNotNull(
                KotlinDependencyScope.API_SCOPE,
                KotlinDependencyScope.IMPLEMENTATION_SCOPE.takeIf { isNativeSharedSourceSet }
            )
            val sourceSetsToIncludeDependencies =
                if (isNativeSharedSourceSet)
                    dependsOnClosureWithInterCompilationDependencies(sourceSet).plus(sourceSet)
                else listOf(sourceSet)
            val sourceSetExportedDependencies = scopes.flatMap { scope ->
                sourceSetsToIncludeDependencies.flatMap { hierarchySourceSet ->
                    project.configurations.sourceSetDependencyConfigurationByScope(hierarchySourceSet, scope).allDependencies.toList()
                }
            }
            sourceSet.name to sourceSetExportedDependencies.map { ModuleIds.fromDependency(it) }.toSet()
        },
        sourceSetCInteropMetadataDirectory = sourceSetsWithMetadataCompilations.keys
            .filter { it.isNativeSourceSet.getOrThrow() }
            .associate { sourceSet -> sourceSet.name to cinteropMetadataDirectoryPath(sourceSet.name) },
        hostSpecificSourceSets = project.future { getHostSpecificSourceSets(project) }.getOrThrow()
            .filter { it in sourceSetsWithMetadataCompilations }.map { it.name }
            .toSet(),
        sourceSetBinaryLayout = sourceSetsWithMetadataCompilations.keys.associate { sourceSet ->
            sourceSet.name to SourceSetMetadataLayout.chooseForProducingProject()
        },
        isPublishedAsRoot = true,
        sourceSetNames = sourceSetsWithMetadataCompilations.keys.map { it.name }.toSet(),
    )
}

internal fun buildProjectStructureMetadata(module: GradleKpmModule): KotlinProjectStructureMetadata {
    val kotlinVariantToGradleVariantNames = module.variants.associate { it.name to it.gradleVariantNames }

    fun <T> expandVariantKeys(map: Map<String, T>) =
        map.entries.flatMap { (key, value) ->
            kotlinVariantToGradleVariantNames[key].orEmpty().plus(key).map { it to value }
        }.toMap()

    val kotlinFragmentsPerKotlinVariant =
        module.variants.associate { variant -> variant.name to variant.withRefinesClosure.map { it.name }.toSet() }
    val fragmentRefinesRelation =
        module.fragments.associate { it.name to it.declaredRefinesDependencies.map { it.fragmentName }.toSet() }

    // FIXME: support native implementation-as-api-dependencies
    // FIXME: support dependencies on auxiliary modules
    val fragmentDependencies =
        module.fragments.associate { fragment ->
            fragment.name to fragment.declaredModuleDependencies.map {
                ModuleIds.lossyFromModuleIdentifier(module.project, it.moduleIdentifier)
            }.toSet()
        }

    return KotlinProjectStructureMetadata(
        sourceSetNamesByVariantName = expandVariantKeys(kotlinFragmentsPerKotlinVariant),
        sourceSetsDependsOnRelation = fragmentRefinesRelation,
        sourceSetBinaryLayout = module.fragments.associate { it.name to SourceSetMetadataLayout.KLIB },
        sourceSetModuleDependencies = fragmentDependencies,
        sourceSetCInteropMetadataDirectory = emptyMap(), // Not supported yet
        hostSpecificSourceSets = module.project.future { getHostSpecificFragments(module).mapTo(mutableSetOf()) { it.name } }.getOrThrow(),
        isPublishedAsRoot = true,
        sourceSetNames = module.fragments.map { it.name }.toSet()
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

        value(PUBLISHED_AS_ROOT_NAME, isPublishedAsRoot.toString())

        multiNodes(VARIANTS_NODE_NAME) {
            sourceSetNamesByVariantName.forEach { (variantName, sourceSets) ->
                multiNodesItem(VARIANT_NODE_NAME) {
                    value(NAME_NODE_NAME, variantName)
                    multiValue(SOURCE_SET_NODE_NAME, sourceSets.toList())
                }
            }
        }

        multiNodes(SOURCE_SETS_NODE_NAME) {
            for (sourceSet in sourceSetNames) {
                multiNodesItem(SOURCE_SET_NODE_NAME) {
                    value(NAME_NODE_NAME, sourceSet)
                    multiValue(DEPENDS_ON_NODE_NAME, sourceSetsDependsOnRelation[sourceSet].orEmpty().toList())
                    multiValue(MODULE_DEPENDENCY_NODE_NAME, sourceSetModuleDependencies[sourceSet].orEmpty().map { moduleDependency ->
                        moduleDependency.groupId + ":" + moduleDependency.moduleId
                    })
                    sourceSetCInteropMetadataDirectory[sourceSet]?.let { cinteropMetadataDirectory ->
                        value(SOURCE_SET_CINTEROP_METADATA_NODE_NAME, cinteropMetadataDirectory)
                    }
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

internal fun parseKotlinSourceSetMetadataFromJson(string: String): KotlinProjectStructureMetadata {
    @Suppress("DEPRECATION") // The replacement doesn't compile against old dependencies such as AS 4.0
    val json = JsonParser().parse(string).asJsonObject
    val valueNamed: JsonObject.(String) -> String? = { name -> get(name)?.asString }
    val multiObjects: JsonObject.(String?) -> Iterable<JsonObject> = { name -> get(name).asJsonArray.map { it.asJsonObject } }
    val multiValues: JsonObject.(String?) -> Iterable<String> = { name -> get(name).asJsonArray.map { it.asString } }

    return parseKotlinSourceSetMetadata({ json.get(ROOT_NODE_NAME).asJsonObject }, valueNamed, multiObjects, multiValues)
}

internal fun parseKotlinSourceSetMetadataFromXml(document: Document): KotlinProjectStructureMetadata {
    val nodeNamed: Element.(String) -> Element? = { name -> getElementsByTagName(name).elements.singleOrNull() }
    val valueNamed: Element.(String) -> String? =
        { name -> getElementsByTagName(name).run { if (length > 0) item(0).textContent else null } }
    val multiObjects: Element.(String) -> Iterable<Element> = { name -> nodeNamed(name)?.childNodes?.elements ?: emptyList() }
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
): KotlinProjectStructureMetadata {
    val projectStructureNode = getRoot()

    val formatVersion = checkNotNull(projectStructureNode.valueNamed(FORMAT_VERSION_NODE_NAME))
    val variantsNode = projectStructureNode.multiObjects(VARIANTS_NODE_NAME)

    val isPublishedAsRoot = projectStructureNode.valueNamed(PUBLISHED_AS_ROOT_NAME)?.toBoolean() ?: false
    val sourceSetsByVariant = mutableMapOf<String, Set<String>>()

    variantsNode.forEach { variantNode ->
        val variantName = requireNotNull(variantNode.valueNamed(NAME_NODE_NAME))
        val sourceSets = variantNode.multiValues(SOURCE_SET_NODE_NAME).toSet()

        sourceSetsByVariant[variantName] = sourceSets
    }

    val sourceSetDependsOnRelation = mutableMapOf<String, Set<String>>()
    val sourceSetModuleDependencies = mutableMapOf<String, Set<ModuleDependencyIdentifier>>()
    val sourceSetBinaryLayout = mutableMapOf<String, SourceSetMetadataLayout>()
    val sourceSetCInteropMetadataDirectory = mutableMapOf<String, String>()
    val hostSpecificSourceSets = mutableSetOf<String>()
    val sourceSetNames = mutableSetOf<String>()

    val sourceSetsNode = projectStructureNode.multiObjects(SOURCE_SETS_NODE_NAME)

    sourceSetsNode.forEach { sourceSetNode ->
        val sourceSetName = checkNotNull(sourceSetNode.valueNamed(NAME_NODE_NAME))
        sourceSetNames.add(sourceSetName)

        val dependsOn = sourceSetNode.multiValues(DEPENDS_ON_NODE_NAME).toSet()
        val moduleDependencies = sourceSetNode.multiValues(MODULE_DEPENDENCY_NODE_NAME).mapTo(mutableSetOf()) {
            val (groupId, moduleId) = it.split(":")
            ModuleDependencyIdentifier(groupId, moduleId)
        }

        sourceSetNode.valueNamed(SOURCE_SET_CINTEROP_METADATA_NODE_NAME)?.let { cinteropMetadataDirectory ->
            sourceSetCInteropMetadataDirectory[sourceSetName] = cinteropMetadataDirectory
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
        sourceSetNamesByVariantName = sourceSetsByVariant,
        sourceSetsDependsOnRelation = sourceSetDependsOnRelation,
        sourceSetBinaryLayout = sourceSetBinaryLayout,
        sourceSetModuleDependencies = sourceSetModuleDependencies,
        sourceSetCInteropMetadataDirectory = sourceSetCInteropMetadataDirectory,
        hostSpecificSourceSets = hostSpecificSourceSets,
        isPublishedAsRoot = isPublishedAsRoot,
        sourceSetNames = sourceSetNames,
        formatVersion = formatVersion
    )
}

internal object GlobalProjectStructureMetadataStorage {
    private const val propertyPrefix = "kotlin.projectStructureMetadata.build"

    fun propertyName(buildName: String, projectPath: String) = "$propertyPrefix.$buildName.path.$projectPath"

    fun registerProjectStructureMetadata(project: Project, metadataProvider: () -> KotlinProjectStructureMetadata) {
        project.compositeBuildRootProject.extensions.extraProperties.set(
            propertyName(project.currentBuildId().name, project.path),
            { metadataProvider().toJson() }
        )
    }

    fun getProjectStructureMetadataProvidersFromAllGradleBuilds(project: Project): Map<ProjectPathWithBuildName, Lazy<KotlinProjectStructureMetadata?>> {
        return project.compositeBuildRootProject.extensions.extraProperties.properties
            .filterKeys { it.startsWith(propertyPrefix) }
            .entries
            .associate { (propertyName, propertyValue) ->
                Pair(
                    propertyName.toProjectPathWithBuildName(),
                    lazy { propertyValue?.getProjectStructureMetadataOrNull() }
                )
            }
    }

    private fun Any.getProjectStructureMetadataOrNull(): KotlinProjectStructureMetadata? {
        val jsonStringProvider = this as? Function0<*> ?: return null
        val jsonString = jsonStringProvider.invoke() as? String ?: return null
        return parseKotlinSourceSetMetadataFromJson(jsonString)
    }

    private fun String.toProjectPathWithBuildName(): ProjectPathWithBuildName {
        val (buildName, projectPath) = removePrefix("$propertyPrefix.").split(".path.")
        return ProjectPathWithBuildName(
            projectPath = projectPath,
            buildName = buildName
        )
    }
}

private const val ROOT_NODE_NAME = "projectStructure"
private const val PUBLISHED_AS_ROOT_NAME = "isPublishedAsRoot"
private const val FORMAT_VERSION_NODE_NAME = "formatVersion"
private const val VARIANTS_NODE_NAME = "variants"
private const val VARIANT_NODE_NAME = "variant"
private const val NAME_NODE_NAME = "name"
private const val SOURCE_SETS_NODE_NAME = "sourceSets"
private const val SOURCE_SET_NODE_NAME = "sourceSet"
private const val SOURCE_SET_CINTEROP_METADATA_NODE_NAME = "sourceSetCInteropMetadataDirectory"
private const val DEPENDS_ON_NODE_NAME = "dependsOn"
private const val MODULE_DEPENDENCY_NODE_NAME = "moduleDependency"
private const val BINARY_LAYOUT_NODE_NAME = "binaryLayout"
private const val HOST_SPECIFIC_NODE_NAME = "hostSpecific"
