/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.attributes.*
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.*
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addIfNotNull

internal const val PRIMARY_SINGLE_COMPONENT_NAME = "kotlin"

abstract class AbstractKotlinTarget(
    final override val project: Project
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

    override val components: Set<SoftwareComponent> by lazy {
        project.buildAdhocComponentsFromKotlinVariants(kotlinComponents)
    }

    protected open fun createKotlinVariant(
        componentName: String,
        compilation: KotlinCompilation<*>,
        usageContexts: Set<DefaultKotlinUsageContext>
    ): KotlinVariant {
        val kotlinExtension = project.kotlinExtension

        val result =
            if (kotlinExtension !is KotlinMultiplatformExtension || targetName == KotlinMultiplatformPlugin.METADATA_TARGET_NAME)
                KotlinVariantWithCoordinates(compilation, usageContexts)
            else {
                val metadataTarget =
                    kotlinExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME) as AbstractKotlinTarget

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
            (RUNTIME to runtimeElementsConfigurationName).takeIf { producingCompilation is KotlinCompilationToRunnableFiles }
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

    override var preset: KotlinTargetPreset<out KotlinTarget>? = null
        internal set
}

private val publishedConfigurationNameSuffix = "-published"

internal fun publishedConfigurationName(originalVariantName: String) = originalVariantName + publishedConfigurationNameSuffix
internal fun originalVariantNameFromPublished(publishedConfigurationName: String): String? =
    publishedConfigurationName.takeIf { it.endsWith(publishedConfigurationNameSuffix) }?.removeSuffix(publishedConfigurationNameSuffix)

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    lowerCamelCaseName(targetName, simpleName)

internal fun javaApiUsageForMavenScoping() = "java-api-jars"

internal fun Project.buildAdhocComponentsFromKotlinVariants(kotlinVariants: Set<KotlinTargetComponent>): Set<SoftwareComponent> {
    val softwareComponentFactoryClass = SoftwareComponentFactory::class.java
    // TODO replace internal API access with injection (not possible until we have this class on the compile classpath)
    val softwareComponentFactory = (project as ProjectInternal).services.get(softwareComponentFactoryClass)

    return kotlinVariants.map { kotlinVariant ->
        val adhocVariant = softwareComponentFactory.adhoc(kotlinVariant.name)

        project.launchInStage(KotlinPluginLifecycle.Stage.AfterFinaliseCompilations) {
            (kotlinVariant as SoftwareComponentInternal).usages.filterIsInstance<KotlinUsageContext>().forEach { kotlinUsageContext ->
                val publishedConfigurationName = publishedConfigurationName(kotlinUsageContext.name)
                val configuration = project.configurations.findByName(publishedConfigurationName)
                    ?: project.configurations.create(publishedConfigurationName).also { configuration ->
                        configuration.isCanBeConsumed = false
                        configuration.isCanBeResolved = false
                        configuration.extendsFrom(project.configurations.getByName(kotlinUsageContext.dependencyConfigurationName))
                        configuration.artifacts.addAll(kotlinUsageContext.artifacts)

                        val attributes = kotlinUsageContext.attributes
                        attributes.keySet().forEach {
                            // capture type parameter T
                            fun <T> copyAttribute(key: Attribute<T>, from: AttributeContainer, to: AttributeContainer) {
                                to.attribute(key, from.getAttribute(key)!!)
                            }
                            copyAttribute(it, attributes, configuration.attributes)
                        }
                    }

                adhocVariant.addVariantsFromConfiguration(configuration) { configurationVariantDetails ->
                    val mavenScope = kotlinUsageContext.mavenScope
                    if (mavenScope != null) {
                        val mavenScopeString = when (mavenScope) {
                            COMPILE -> "compile"
                            RUNTIME -> "runtime"
                        }
                        configurationVariantDetails.mapToMavenScope(mavenScopeString)
                    }
                }
            }
        }

        adhocVariant as SoftwareComponent

        object : ComponentWithVariants, ComponentWithCoordinates, SoftwareComponentInternal {
            override fun getCoordinates() =
                (kotlinVariant as? ComponentWithCoordinates)?.coordinates ?: error("kotlinVariant is not ComponentWithCoordinates")

            override fun getVariants(): Set<SoftwareComponent> =
                (kotlinVariant as? KotlinVariantWithMetadataVariant)?.variants.orEmpty()

            override fun getName(): String = adhocVariant.name
            override fun getUsages(): MutableSet<out UsageContext> = (adhocVariant as SoftwareComponentInternal).usages
        }
    }.toSet()
}
