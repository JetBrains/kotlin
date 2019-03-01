/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.mpp

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DomainObjectSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurablePublishArtifact
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage.JAVA_API
import org.gradle.api.attributes.Usage.JAVA_RUNTIME_JARS
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.isGradleVersionAtLeast
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

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

    override val components: Set<KotlinTargetComponent> by lazy {
        val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
        val usageContexts = createUsageContexts(mainCompilation)
        setOf(
            if (isGradleVersionAtLeast(4, 7)) {
                val componentName = mainCompilation.target.name
                createKotlinVariant(componentName, mainCompilation, usageContexts).apply {
                    sourcesArtifacts = setOf(
                        sourcesJarArtifact(
                            mainCompilation, componentName,
                            dashSeparatedName(target.name.toLowerCase(), componentName.toLowerCase())
                        )
                    )
                }
            } else {
                KotlinVariant(mainCompilation, usageContexts)
            }
        ).also { project.components.addAll(it) }
    }

    protected fun createKotlinVariant(
        componentName: String,
        compilation: KotlinCompilation<*>,
        usageContexts: Set<DefaultKotlinUsageContext>
    ): KotlinVariant {

        val kotlinExtension = project.kotlinExtension as? KotlinMultiplatformExtension
            ?: return KotlinVariantWithCoordinates(compilation, usageContexts)

        if (targetName == KotlinMultiplatformPlugin.METADATA_TARGET_NAME)
            return KotlinVariantWithCoordinates(compilation, usageContexts)

        val separateMetadataTarget = kotlinExtension.targets.getByName(KotlinMultiplatformPlugin.METADATA_TARGET_NAME)

        val result = if (kotlinExtension.isGradleMetadataAvailable) {
            KotlinVariantWithMetadataVariant(compilation, usageContexts, separateMetadataTarget)
        } else {
            // we should only add the Kotlin metadata dependency if we publish no Gradle metadata related to Kotlin MPP;
            // with metadata, such a dependency would get invalid, since a platform module should only depend on modules for that
            // same platform, not Kotlin metadata modules
            KotlinVariantWithMetadataDependency(compilation, usageContexts, separateMetadataTarget)
        }

        result.componentName = componentName
        return result
    }

    private fun createUsageContexts(
        producingCompilation: KotlinCompilation<*>
    ): Set<DefaultKotlinUsageContext> {
        // Here, `JAVA_API` and `JAVA_RUNTIME_JARS` are used intentionally as Gradle needs this for
        // ordering of the usage contexts (prioritizing the dependencies) when merging them into the POM;
        // These Java usages should not be replaced with the custom Kotlin usages.
        return listOfNotNull(
            JAVA_API to apiElementsConfigurationName,
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

