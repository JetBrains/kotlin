/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.provider.Provider
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.PRESETS_API_IS_DEPRECATED_MESSAGE
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.COMPILE
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.RUNTIME
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull
import java.nio.file.Path

internal const val PRIMARY_SINGLE_COMPONENT_NAME = "kotlin"

abstract class AbstractKotlinTarget(
    final override val project: Project,
) : InternalKotlinTarget {

    final override val extras: MutableExtras = mutableExtrasOf()

    private val attributeContainer = HierarchyAttributeContainer(parent = null)

    override fun getAttributes(): AttributeContainer = attributeContainer

    @Deprecated("Scheduled for removal with Kotlin 2.2")
    override var useDisambiguationClassifierAsSourceSetNamePrefix: Boolean = true
        internal set

    @Deprecated("Scheduled for removal with Kotlin 2.2")
    override var overrideDisambiguationClassifierOnIdeImport: String? = null
        internal set

    override val apiElementsConfigurationName: String
        get() = disambiguateName("apiElements")

    override val runtimeElementsConfigurationName: String
        get() = disambiguateName("runtimeElements")

    override val sourcesElementsConfigurationName: String
        get() = disambiguateName("sourcesElements")

    override val resourcesElementsConfigurationName: String
        get() = disambiguateName("resourcesElements")

    override val artifactsTaskName: String
        get() = disambiguateName("jar")

    override fun toString(): String = "target $name ($platformType)"

    override val publishable: Boolean
        get() = true

    override var isSourcesPublishable: Boolean = true
    override fun withSourcesJar(publish: Boolean) {
        isSourcesPublishable = publish
    }

    @InternalKotlinGradlePluginApi
    override val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val usageContexts = createUsageContexts(mainCompilation).toMutableSet()

        val componentName =
            if (project.kotlinExtension is KotlinMultiplatformExtension)
                targetName
            else PRIMARY_SINGLE_COMPONENT_NAME

        usageContexts.addIfNotNull(
            createSourcesJarAndUsageContextIfPublishable(
                producingCompilation = mainCompilation,
                componentName = componentName,
                artifactNameAppendix = dashSeparatedName(targetName.toLowerCaseAsciiOnly())
            )
        )

        val result = createKotlinVariant(componentName, mainCompilation, usageContexts)

        setOf(result)
    }


    /**
     * Returns, potentially not configured (e.g. without some usages), Gradle SoftwareComponent's for this target
     * For final version of components use [awaitComponents]
     */
    override val components: Set<KotlinTargetSoftwareComponent> by lazy {
        kotlinComponents.map { kotlinComponent -> KotlinTargetSoftwareComponent(this, kotlinComponent) }.toSet()
    }

    protected open fun createKotlinVariant(
        componentName: String,
        compilation: KotlinCompilation<*>,
        usageContexts: Set<DefaultKotlinUsageContext>
    ): KotlinVariant {
        val kotlinExtension = project.kotlinExtension

        val result =
            if (kotlinExtension !is KotlinMultiplatformExtension || targetName == KotlinMetadataTarget.METADATA_TARGET_NAME)
                KotlinVariantWithCoordinates(compilation, usageContexts)
            else {
                val metadataTarget =
                    kotlinExtension.targets.getByName(KotlinMetadataTarget.METADATA_TARGET_NAME) as AbstractKotlinTarget

                KotlinVariantWithMetadataVariant(compilation, usageContexts, metadataTarget)
            }

        result.componentName = componentName
        return result
    }

    internal open fun createUsageContexts(
        producingCompilation: KotlinCompilation<*>
    ): Set<DefaultKotlinUsageContext> {
        return listOfNotNull(
            COMPILE to apiElementsConfigurationName,
            (RUNTIME to runtimeElementsConfigurationName).takeIf {
                @Suppress("DEPRECATION")
                producingCompilation is KotlinCompilationToRunnableFiles
            }
        ).mapTo(mutableSetOf()) { (mavenScope, dependenciesConfigurationName) ->
            DefaultKotlinUsageContext(
                producingCompilation,
                mavenScope,
                dependenciesConfigurationName
            )
        }
    }

    protected fun createSourcesJarAndUsageContextIfPublishable(
        producingCompilation: KotlinCompilation<*>,
        componentName: String,
        artifactNameAppendix: String,
        classifierPrefix: String? = null,
        sourcesElementsConfigurationName: String = this.sourcesElementsConfigurationName,
        overrideConfigurationAttributes: AttributeContainer? = null,
        mavenScope: KotlinUsageContext.MavenScope? = null,
    ): DefaultKotlinUsageContext? {
        // We want to create task anyway, even if sources are not going to be published by KGP
        // So users or other plugins can still use it
        val sourcesJarTask = sourcesJarTask(producingCompilation, componentName, artifactNameAppendix)
        if (!isSourcesPublishable) return null

        // If sourcesElements configuration not found, don't create artifact.
        // This can happen in pure JVM plugin where source publication is delegated to Java Gradle Plugin.
        // But we still want to have sourcesJarTask be registered
        project.configurations.findByName(sourcesElementsConfigurationName) ?: return null

        val artifact = project.artifacts.add(sourcesElementsConfigurationName, sourcesJarTask) as ConfigurablePublishArtifact
        artifact.classifier = dashSeparatedName(classifierPrefix, "sources")

        return DefaultKotlinUsageContext(
            compilation = producingCompilation,
            dependencyConfigurationName = sourcesElementsConfigurationName,
            overrideConfigurationAttributes = overrideConfigurationAttributes,
            mavenScope = mavenScope,
            includeIntoProjectStructureMetadata = false,
            publishOnlyIf = { isSourcesPublishable }
        )
    }

    protected fun createResourcesSoftwareComponentVariant(
        producingCompilation: KotlinCompilation<*>,
    ): DefaultKotlinUsageContext {
        return DefaultKotlinUsageContext(
            compilation = producingCompilation,
            dependencyConfigurationName = resourcesElementsConfigurationName,
            // FIXME: ???
            includeIntoProjectStructureMetadata = false,
            // FIXME: Should we publish this variant only if there is something to publish? Why does configuration also have a setting to control publication?
            publishOnlyIf = { true }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private val publicationConfigureActions: DomainObjectSet<Action<MavenPublication>> = project.objects
        .domainObjectSet(Action::class.java) as DomainObjectSet<Action<MavenPublication>>

    override fun mavenPublication(action: Action<MavenPublication>) {
        publicationConfigureActions.add(action)
    }

    @InternalKotlinGradlePluginApi
    override fun onPublicationCreated(publication: MavenPublication) {
        publicationConfigureActions.all { action -> action.execute(publication) }
    }

    @OptIn(DeprecatedTargetPresetApi::class)
    @Deprecated(
        PRESETS_API_IS_DEPRECATED_MESSAGE,
        level = DeprecationLevel.WARNING,
    )
    override var preset: KotlinTargetPreset<out KotlinTarget>? = null
        internal set

    internal val composeResourceDirectories: MutableList<ComposeResources> = mutableListOf()

    // FIXME: Check these paths are actually relative
    override fun composeCopyResources(
        resourceDirectoryPathRelativeToSourceSet: Provider<Path>,
        resourcePlacementPathRelativeToPublicationRoot: Provider<Path>,
        resourceTypeTaskNameSuffix: String
    ) {
        composeResourceDirectories.add(
            ComposeResources(
                resourceDirectoryPathRelativeToSourceSet,
                resourcePlacementPathRelativeToPublicationRoot,
                resourceTypeTaskNameSuffix
            )
        )
    }

    override fun composeResolveResources(): TaskProvider<*> {
        val outputDirectory = project.layout.buildDirectory.dir("${disambiguationClassifier}ResolvedResources")
        // FIXME: Pass the configuration name from somewhere
        val resourcesConfiguration = project.configurations.getByName(
            lowerCamelCaseName(
                disambiguationClassifier, "ResourcesPath"
            )
        )

        // Get resources from dependencies
        val unzipAndCopyResourcesToBuildDirectoryTask = project.locateOrRegisterTask<Copy>("${disambiguationClassifier}ResolveResources") {
            with(it) {
                // Depend on the resources configuration for multi-project builds
                dependsOn(resourcesConfiguration)
                into(outputDirectory)

                // FIXME: Why does this need to be lazy? Should there be an explicit await here?
                from({
                    // FIXME: Can lenient resolve hide unexpected failures? Can we avoid this?
                     resourcesConfiguration.incoming.artifactView { it.lenient(true) }.files
                         .filter {
                             // FIXME: Don't output an artifact if there are no resources?
                             it.exists()
                             // FIXME: Why does wasm pass in self classes directory in this configuration? Check if the file is a zip?
                             && it.isFile
                         }
                         .map {
                             // Copy the root of each zip to the build directory
                             project.zipTree(it)
                         }
                })
            }
        }

        val aggregatedResourcesOutputTask = project.registerTask<DefaultTask>("${disambiguationClassifier}AggregateResources") {
            // FIXME: Output directory may contain junk
            it.outputs.dir(outputDirectory)
        }
        aggregatedResourcesOutputTask.dependsOn(unzipAndCopyResourcesToBuildDirectoryTask)

        // FIXME: Resources and outputDirectory are Providers, but this method doesn't await for any lifecycle event. This will break if called at a wrong time
        project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseRefinesEdges) {
            compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).registerCopyComposeResourcesTasks(
                "${disambiguationClassifier}ResolveSelfResources",
                composeResourceDirectories,
                outputDirectory
            ).forEach { copyTask ->
                aggregatedResourcesOutputTask.dependsOn(copyTask)
            }
        }

        // FIXME: Listing files in this directory may result in trash such as .DS_Store to suddenly appear. Figure out how to get the root of a zipTree?
        return aggregatedResourcesOutputTask
    }
}

internal data class ComposeResources(
    val resourceDirectoryPathRelativeToSourceSet: Provider<Path>,
    val resourcePlacementPathRelativeToPublicationRoot: Provider<Path>,
    val resourceTypeTaskNameSuffix: String,
)

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    lowerCamelCaseName(targetName, simpleName)

