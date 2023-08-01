/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.HierarchyAttributeContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinTarget
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

internal class ExternalKotlinTargetImpl internal constructor(
    override val project: Project,
    override val targetName: String,
    override val platformType: KotlinPlatformType,
    override val publishable: Boolean,
    override val compilerOptions: KotlinCommonCompilerOptions,
    val apiElementsConfiguration: Configuration,
    val runtimeElementsConfiguration: Configuration,
    val sourcesElementsConfiguration: Configuration,
    val apiElementsPublishedConfiguration: Configuration,
    val runtimeElementsPublishedConfiguration: Configuration,
    val sourcesElementsPublishedConfiguration: Configuration,
    val kotlinTargetComponent: ExternalKotlinTargetComponent,
    private val artifactsTaskLocator: ArtifactsTaskLocator,
) : InternalKotlinTarget {


    fun interface ArtifactsTaskLocator {
        fun locate(target: ExternalKotlinTargetImpl): TaskProvider<out Task>
    }

    val kotlin = project.multiplatformExtension

    override val extras: MutableExtras = mutableExtrasOf()

    override val preset: Nothing? = null

    internal val logger: Logger = Logging.getLogger("${ExternalKotlinTargetImpl::class.qualifiedName}: $name")

    override val useDisambiguationClassifierAsSourceSetNamePrefix: Boolean = true

    override val overrideDisambiguationClassifierOnIdeImport: String? = null

    val artifactsTask: TaskProvider<out Task> by lazy {
        artifactsTaskLocator.locate(this)
    }

    override var isSourcesPublishable: Boolean = true

    override fun withSourcesJar(publish: Boolean) {
        isSourcesPublishable = publish
    }

    override val artifactsTaskName: String
        get() = artifactsTask.name

    override val apiElementsConfigurationName: String
        get() = apiElementsConfiguration.name

    override val runtimeElementsConfigurationName: String
        get() = runtimeElementsConfiguration.name

    override val sourcesElementsConfigurationName: String
        get() = sourcesElementsConfiguration.name

    @InternalKotlinGradlePluginApi
    override val kotlinComponents: Set<KotlinTargetComponent> = setOf(kotlinTargetComponent)

    override val components: Set<ExternalKotlinTargetSoftwareComponent> by lazy {
        logger.debug("Creating SoftwareComponent")
        setOf(ExternalKotlinTargetSoftwareComponent(this))
    }

    override val compilations: NamedDomainObjectContainer<DecoratedExternalKotlinCompilation> by lazy {
        project.container(DecoratedExternalKotlinCompilation::class.java)
    }

    @Suppress("unchecked_cast")
    private val mavenPublicationActions = project.objects.domainObjectSet(Action::class.java)
            as DomainObjectSet<Action<MavenPublication>>

    override fun mavenPublication(action: Action<MavenPublication>) {
        mavenPublicationActions.add(action)
    }

    @InternalKotlinGradlePluginApi
    override fun onPublicationCreated(publication: MavenPublication) {
        mavenPublicationActions.all { action -> action.execute(publication) }
    }

    private val attributeContainer = HierarchyAttributeContainer(parent = null)

    override fun getAttributes(): AttributeContainer = attributeContainer

    internal fun onCreated() {
        artifactsTask
    }
}
