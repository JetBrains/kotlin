/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.JsonUtils
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.projectStoredProperty
import org.jetbrains.kotlin.gradle.utils.registerArtifact

internal val Project.kotlinSecondaryVariantsDataSharing: KotlinSecondaryVariantsDataSharing by projectStoredProperty {
    KotlinSecondaryVariantsDataSharing(project)
}

/**
 * Marker interface of classes that shares data between Gradle Projects using [KotlinSecondaryVariantsDataSharing]
 * Implementations should be serializable via [JsonUtils]
 */
internal interface KotlinShareableDataAsSecondaryVariant

/**
 * Service to share configuration state between Kotlin Projects as Configuration Secondary Variants.
 * So this data can be consumed during Task Execution. And is trackable via Task Inputs.
 *
 * This is an alternative to BuildServices that are not trackable as Task Inputs.
 *
 */
internal class KotlinSecondaryVariantsDataSharing(
    private val project: Project,
) {
    fun <T : KotlinShareableDataAsSecondaryVariant> shareDataFromProvider(
        key: String,
        outgoingConfiguration: Configuration,
        dataProvider: Provider<T>,
        taskDependencies: List<Any> = emptyList(),
    ) {
        val taskName = lowerCamelCaseName("export", key, "for", outgoingConfiguration.name)
        val task = project.locateOrRegisterTask<ExportKotlinProjectDataTask>(taskName, configureTask = {
            val fileName = "${key}_${outgoingConfiguration.name}.json"

            @Suppress("UNCHECKED_CAST")
            val taskOutputData = outputData as Property<T>
            taskOutputData.set(dataProvider)

            outputFile.set(project.layout.buildDirectory.file("kotlin/kotlin-project-shared-data/$fileName"))
            dependsOn(taskDependencies)
        })

        shareDataFromExistingTask(key, outgoingConfiguration, task.flatMap { it.outputFile })
    }

    private fun shareDataFromExistingTask(
        key: String,
        outgoingConfiguration: Configuration,
        taskOutputProvider: Provider<RegularFile>,
        taskDependencies: List<Any> = emptyList(),
    ) {
        if (outgoingConfiguration.outgoing.variants.names.contains(key)) {
            project.logger.warn(
                "KotlinSecondaryVariantsDataSharing can't create secondary variant with name $key "
                        + "on configuration $outgoingConfiguration. Something can be declared twice or have clashing names. "
                        + "Please report this to https://kotl.in/issue"
            )
            return
        }

        outgoingConfiguration.outgoing.variants.create(key) { variant ->
            variant.registerArtifact(
                artifactProvider = taskOutputProvider,
                type = artifactTypeOfProjectSharedDataKey(key)
            ) {
                builtBy(taskDependencies)
            }
            variant.attributes.configureCommonAttributes(key)
        }
    }

    fun <T : KotlinShareableDataAsSecondaryVariant> consume(
        key: String,
        incomingConfiguration: Configuration,
        clazz: Class<T>,
        componentFilter: ((ComponentIdentifier) -> Boolean)? = null,
    ): KotlinProjectSharedDataProvider<T> {
        val lazyResolvedConfiguration = LazyResolvedConfigurationWithArtifacts(incomingConfiguration, configureArtifactView = {
            attributes.configureCommonAttributes(key)
            // artifactType is set by gradle on the producer side
            // Request it explicitly to bypass Artifact Transformations that gradle may apply
            // see: https://github.com/gradle/gradle/issues/33298
            attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, artifactTypeOfProjectSharedDataKey(key))
            if (componentFilter != null) this.componentFilter(componentFilter)
        })
        return KotlinProjectSharedDataProvider(key, lazyResolvedConfiguration, clazz)
    }

    /** Common attributes between producer and consumer */
    private fun AttributeContainer.configureCommonAttributes(key: String) {
        val usageValue = project.objects.named(Usage::class.java, "kotlin-project-shared-data")
        attributeProvider(Usage.USAGE_ATTRIBUTE, project.provider { usageValue })
        attributeProvider(kotlinProjectSharedDataAttribute, project.provider { key })
    }
}

private val kotlinProjectSharedDataAttribute = Attribute.of("org.jetbrains.kotlin.project-shared-data", String::class.java)

/** Adds namespacing prefix to an arbitrary [key] to prevent misunderstanding in outgoing variants */
private fun artifactTypeOfProjectSharedDataKey(key: String) = "kotlin-project-shared-data-$key"

/**
 * Represents a provider that can extract some [T] that was published by a project in the current build as
 * a secondary variant via [kotlinSecondaryVariantsDataSharing].
 * And the current (consumer) project has dependency to the producer project.
 *
 * Data is meant to be extracted at Task Execution Phase.
 *
 * This class is Configuration Cache safe. It can be stored in a Task field.
 */
internal class KotlinProjectSharedDataProvider<T : KotlinShareableDataAsSecondaryVariant>(
    private val key: String,
    private val lazyResolvedConfiguration: LazyResolvedConfigurationWithArtifacts,
    private val clazz: Class<T>,
) {
    val rootComponent: ResolvedComponentResult get() = lazyResolvedConfiguration.root

    val allResolvedDependencies: Set<ResolvedDependencyResult> get() = lazyResolvedConfiguration.allResolvedDependencies

    val files: FileCollection = lazyResolvedConfiguration.files

    fun getProjectDataFromDependencyOrNull(resolvedDependency: ResolvedDependencyResult): T? {
        val artifact = lazyResolvedConfiguration.getArtifacts(resolvedDependency).singleOrNull() ?: return null
        return artifact.parse()
    }

    private fun ResolvedArtifactResult.parse(): T? {
        // In rare cases, for example when provided attributes and requested attributes didn't match at all.
        // Gradle will resolve into that variant.
        // It can happen in Android Gradle Plugin for example, as they have a lot of secondary variants with few attributes
        val keyFromResolvedArtifact = variant.attributes.getAttribute(kotlinProjectSharedDataAttribute) ?: return null
        if (key != keyFromResolvedArtifact) return null

        // In some cases, for example CInterop transformations for IDE, actual artifact file may not exists
        // this can happen because consuming task doesn't not depend on the resolved artifacts collection
        // and producing task may not be invoked. So checking for file existence is required here.
        if (!file.exists()) return null

        val content = file.readText()
        return runCatching { JsonUtils.gson.fromJson(content, clazz) }.getOrNull()
    }
}

@DisableCachingByDefault(because = "Trivial operation")
internal abstract class ExportKotlinProjectDataTask : DefaultTask() {
    @get:Nested
    abstract val outputData: Property<KotlinShareableDataAsSecondaryVariant>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun action() {
        val data = outputData.get()
        val json = JsonUtils.gson.toJson(data)
        outputFile.get().asFile.writeText(json)
    }
}