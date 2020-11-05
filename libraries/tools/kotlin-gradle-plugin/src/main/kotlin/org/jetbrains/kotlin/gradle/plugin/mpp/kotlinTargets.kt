/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage.JAVA_RUNTIME_JARS
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal const val PRIMARY_SINGLE_COMPONENT_NAME = "kotlin"

abstract class AbstractKotlinTarget(
    final override val project: Project
) : KotlinTarget {
    private val attributeContainer = HierarchyAttributeContainer(parent = null)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val defaultConfigurationName: String
        get() = disambiguateName("default")

    override var useDisambiguationClassifierAsSourceSetNamePrefix: Boolean = true
        internal set

    override var overrideDisambiguationClassifierOnIdeImport: String? = null
        internal set

    override val apiElementsConfigurationName: String
        get() = disambiguateName("apiElements")

    override val runtimeElementsConfigurationName: String
        get() = disambiguateName("runtimeElements")

    override val artifactsTaskName: String
        get() = disambiguateName("jar")

    override fun toString(): String = "target $name ($platformType)"

    override val publishable: Boolean
        get() = true

    internal open val kotlinComponents: Set<KotlinTargetComponent> by lazy {
        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val usageContexts = createUsageContexts(mainCompilation)

        val componentName =
            if (project.kotlinExtension is KotlinMultiplatformExtension)
                targetName
            else PRIMARY_SINGLE_COMPONENT_NAME

        val result = createKotlinVariant(componentName, mainCompilation, usageContexts)

        result.sourcesArtifacts = setOf(
            sourcesJarArtifact(mainCompilation, componentName, dashSeparatedName(targetName.toLowerCase()))
        )

        setOf(result)
    }

    override val components: Set<SoftwareComponent> by lazy {
        buildAdhocComponentsFromKotlinVariants(kotlinComponents)
    }

    private fun buildAdhocComponentsFromKotlinVariants(kotlinVariants: Set<KotlinTargetComponent>): Set<SoftwareComponent> {
        val softwareComponentFactoryClass = SoftwareComponentFactory::class.java
        // TODO replace internal API access with injection (not possible until we have this class on the compile classpath)
        val softwareComponentFactory = (project as ProjectInternal).services.get(softwareComponentFactoryClass)

        return kotlinVariants.map { kotlinVariant ->
            val adhocVariant = softwareComponentFactory.adhoc(kotlinVariant.name)

            project.whenEvaluated {
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
                        val mavenScope = when (kotlinUsageContext.usage.name) {
                            "java-api-jars" -> "compile"
                            "java-runtime-jars" -> "runtime"
                            else -> error("unexpected usage value '${kotlinUsageContext.usage.name}'")
                        }
                        configurationVariantDetails.mapToMavenScope(mavenScope)
                    }
                }
            }

            adhocVariant as SoftwareComponent

            // Resolve the values early here instead of passing `adhocVariant` to the SoftwareComponent object below (which would break
            // configuration caching---see KT-43054). Passing `kotlinVariant` to the object did not break configuration caching, but we also
            // avoid doing that just to be safe.
            val coordinates = (kotlinVariant as? ComponentWithCoordinates)?.coordinates
            val variants = (kotlinVariant as? KotlinVariantWithMetadataVariant)?.variants.orEmpty()
            val name = adhocVariant.name
            val usages = (adhocVariant as SoftwareComponentInternal).usages

            object : ComponentWithVariants, ComponentWithCoordinates, SoftwareComponentInternal {
                override fun getCoordinates() = coordinates
                override fun getVariants(): Set<out SoftwareComponent> = variants
                override fun getName(): String = name
                override fun getUsages(): MutableSet<out UsageContext> = usages
            }
        }.toSet()
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
        // Here, the Java Usage values are used intentionally as Gradle needs this for
        // ordering of the usage contexts (prioritizing the dependencies) when merging them into the POM;
        // These Java usages should not be replaced with the custom Kotlin usages.
        return listOfNotNull(
            javaApiUsageForMavenScoping() to apiElementsConfigurationName,
            (JAVA_RUNTIME_JARS to runtimeElementsConfigurationName).takeIf { producingCompilation is KotlinCompilationToRunnableFiles }
        ).mapTo(mutableSetOf()) { (usageName, dependenciesConfigurationName) ->
            DefaultKotlinUsageContext(
                producingCompilation,
                project.usageByName(usageName),
                dependenciesConfigurationName
            )
        }
    }

    protected fun sourcesJarArtifact(
        producingCompilation: KotlinCompilation<*>,
        componentName: String,
        artifactNameAppendix: String,
        classifierPrefix: String? = null
    ): PublishArtifact {
        val sourcesJarTask = sourcesJarTask(producingCompilation, componentName, artifactNameAppendix)
        val sourceArtifactConfigurationName = producingCompilation.disambiguateName("sourceArtifacts")
        return producingCompilation.target.project.run {
            (configurations.findByName(sourceArtifactConfigurationName) ?: run {
                val configuration = configurations.create(sourceArtifactConfigurationName) {
                    it.isCanBeResolved = false
                    it.isCanBeConsumed = false
                }
                artifacts.add(sourceArtifactConfigurationName, sourcesJarTask)
                configuration
            }).artifacts.single().apply {
                this as ConfigurablePublishArtifact
                classifier = dashSeparatedName(classifierPrefix, "sources")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal val publicationConfigureActions =
        WrapUtil.toDomainObjectSet(Action::class.java) as DomainObjectSet<Action<MavenPublication>>

    override fun mavenPublication(action: Action<MavenPublication>) {
        publicationConfigureActions.add(action)
    }

    override fun mavenPublication(action: Closure<Unit>) =
        mavenPublication(ConfigureUtil.configureUsing(action))

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

abstract class KotlinOnlyTarget<T : KotlinCompilation<*>>(
    project: Project,
    override val platformType: KotlinPlatformType
) : AbstractKotlinTarget(project) {

    override lateinit var compilations: NamedDomainObjectContainer<T>
        internal set

    override lateinit var targetName: String
        internal set

    override var disambiguationClassifier: String? = null
        internal set
}