/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage.JAVA_API
import org.gradle.api.attributes.Usage.JAVA_RUNTIME_JARS
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
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
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal const val PRIMARY_SINGLE_COMPONENT_NAME = "kotlin"

abstract class AbstractKotlinTarget(
    final override val project: Project
) : KotlinTarget {
    private val attributeContainer = HierarchyAttributeContainer(parent = null)

    override fun getAttributes(): AttributeContainer = attributeContainer

    override val defaultConfigurationName: String
        get() = disambiguateName("default")

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

        val result = if (isGradleVersionAtLeast(4, 7)) {
            createKotlinVariant(componentName, mainCompilation, usageContexts)
        } else {
            KotlinVariant(mainCompilation, usageContexts)
        }

        result.sourcesArtifacts = setOf(
            sourcesJarArtifact(mainCompilation, componentName, dashSeparatedName(targetName.toLowerCase()))
        )

        setOf(result)
    }

    override val components: Set<SoftwareComponent> by lazy {
        val kotlinVariants = kotlinComponents
        if (isGradleVersionAtLeast(5, 3)) {
            buildAdhocComponentsFromKotlinVariants(kotlinVariants)
        } else {
            kotlinVariants.also { project.components.addAll(it) }
        }
    }

    // This API is introduced in Gradle 5.3. TODO when we build against Gradle 5.3+, rewrite this function
    private fun buildAdhocComponentsFromKotlinVariants(kotlinVariants: Set<KotlinTargetComponent>): Set<SoftwareComponent> {
        val softwareComponentFactoryClass = Class.forName("org.gradle.api.component.SoftwareComponentFactory")
        // TODO replace internal API access with injection (not possible until we have this class on the compile classpath)
        val softwareComponentFactory = (project as ProjectInternal).services.get(softwareComponentFactoryClass)

        val adhocMethod = softwareComponentFactoryClass.getMethod("adhoc", String::class.java)
        val adhocSoftwareComponentClass = Class.forName("org.gradle.api.component.AdhocComponentWithVariants")
        val addVariantsFromConfigurationMethod = adhocSoftwareComponentClass.getMethod(
            "addVariantsFromConfiguration", Configuration::class.java, org.gradle.api.Action::class.java
        )
        val configurationVariantDetailsClass = Class.forName("org.gradle.api.component.ConfigurationVariantDetails")
        val mapToMavenScopeMethod = configurationVariantDetailsClass.getMethod(
            "mapToMavenScope", String::class.java
        )

        return kotlinVariants.map { kotlinVariant ->
            val adhocVariant = adhocMethod(softwareComponentFactory, kotlinVariant.name)

            project.whenEvaluated {
                (kotlinVariant as SoftwareComponentInternal).usages.filterIsInstance<KotlinUsageContext>().forEach { kotlinUsageContext ->
                    val configuration = project.configurations.findByName(kotlinUsageContext.name)
                        ?: project.configurations.create(kotlinUsageContext.name).also { configuration ->
                            configuration.isCanBeConsumed = false
                            configuration.isCanBeResolved = false
                            configuration.dependencies.addAll(kotlinUsageContext.dependencies)
                            configuration.dependencyConstraints.addAll(kotlinUsageContext.dependencyConstraints)
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

                    val chooseMavenScopeAction = Action<Any> { configurationVariantDetails ->
                        val mavenScope = when (kotlinUsageContext.usage.name) {
                            "java-api-jars" -> "compile"
                            JAVA_RUNTIME_JARS -> "runtime"
                            else -> error("unexpected usage value '${kotlinUsageContext.usage.name}'")
                        }
                        mapToMavenScopeMethod(configurationVariantDetails, mavenScope)
                    }

                    addVariantsFromConfigurationMethod(adhocVariant, configuration, chooseMavenScopeAction)
                }
            }

            adhocVariant as SoftwareComponent

            if (kotlinVariant is KotlinVariantWithMetadataVariant) {
                object : ComponentWithVariants, ComponentWithCoordinates, SoftwareComponentInternal {
                    override fun getCoordinates() = kotlinVariant.coordinates
                    override fun getVariants(): Set<out SoftwareComponent> = kotlinVariant.variants
                    override fun getName(): String = adhocVariant.name
                    override fun getUsages(): MutableSet<out UsageContext> = (adhocVariant as SoftwareComponentInternal).usages
                }
            } else {
                object : ComponentWithCoordinates, SoftwareComponentInternal {
                    override fun getCoordinates() = (kotlinVariant as? ComponentWithCoordinates)?.coordinates
                    override fun getName(): String = adhocVariant.name
                    override fun getUsages(): MutableSet<out UsageContext> = (adhocVariant as SoftwareComponentInternal).usages
                }
            }
        }.toSet()
    }

    protected fun createKotlinVariant(
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

                if (kotlinExtension.isGradleMetadataAvailable) {
                    KotlinVariantWithMetadataVariant(compilation, usageContexts, metadataTarget)
                } else {
                    // we should only add the Kotlin metadata dependency if we publish no Gradle metadata related to Kotlin MPP;
                    // with metadata, such a dependency would get invalid, since a platform module should only depend on modules for that
                    // same platform, not Kotlin metadata modules
                    KotlinVariantWithMetadataDependency(compilation, usageContexts, metadataTarget)
                }
            }

        result.componentName = componentName
        return result
    }

    private fun createUsageContexts(
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

internal fun KotlinTarget.disambiguateName(simpleName: String) =
    lowerCamelCaseName(targetName, simpleName)

internal fun javaApiUsageForMavenScoping() =
    if (isGradleVersionAtLeast(5, 3)) {
        "java-api-jars"
    } else {
        JAVA_API
    }

open class KotlinOnlyTarget<T : KotlinCompilation<*>>(
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

