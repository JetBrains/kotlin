/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValueOrThrow
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.COMPILE
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetComponent.TargetProvider
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.gradle.utils.lazyFuture
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class ExternalKotlinTargetComponent(
    project: Project,
    val targetProvider: TargetProvider,
) : KotlinTargetComponentWithPublication, ComponentWithCoordinates, SoftwareComponentInternal {

    /*
    Target creation requires this component. We will provide the target once it is required
     */
    fun interface TargetProvider {
        operator fun invoke(): DecoratedExternalKotlinTarget

        companion object {
            fun byTargetName(extension: KotlinMultiplatformExtension, targetName: String) = TargetProvider {
                extension.targets.getByName(targetName) as DecoratedExternalKotlinTarget
            }
        }
    }

    /* Required for getting correct coordinates */
    override var publicationDelegate: MavenPublication? = null

    override val target: DecoratedExternalKotlinTarget by lazy { targetProvider() }

    override val publishable: Boolean
        get() = target.publishable

    override val publishableOnCurrentHost: Boolean
        get() = true

    override val defaultArtifactId: String
        get() = dashSeparatedName(target.project.name, target.name.toLowerCaseAsciiOnly())

    @Deprecated(
        message = "Sources artifacts are now published as separate variant " +
                "use target.sourcesElementsConfigurationName to obtain necessary information",
        replaceWith = ReplaceWith("target.sourcesElementsConfigurationName")
    )
    override val sourcesArtifacts: Set<PublishArtifact>
        get() = emptySet()

    override fun getName(): String = target.name

    override fun getCoordinates(): ModuleVersionIdentifier =
        getCoordinatesFromPublicationDelegateAndProject(publicationDelegate, target.project, null)

    val kotlinUsagesFuture = project.future {
        KotlinPluginLifecycle.Stage.FinaliseCompilations.await()

        val compilation = target
            .compilations
            .findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            ?: error("Missing conventional '${KotlinCompilation.MAIN_COMPILATION_NAME}' compilation in '$target'")

        val result = mutableSetOf(
            DefaultKotlinUsageContext(compilation, COMPILE, target.apiElementsPublishedConfiguration.name),
            DefaultKotlinUsageContext(compilation, RUNTIME, target.runtimeElementsPublishedConfiguration.name),
        )

        if (target.isSourcesPublishableProperty.awaitFinalValueOrThrow()) {
            result += DefaultKotlinUsageContext(
                compilation = compilation,
                mavenScope = null,
                dependencyConfigurationName = target.sourcesElementsPublishedConfiguration.name,
                includeIntoProjectStructureMetadata = false
            )
        }

        result
    }

    override fun getUsages(): Set<KotlinUsageContext> = kotlinUsagesFuture.getOrThrow()

    /**
     * Should be used in Gradle's Publication only.
     * See [org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponent.getVariants]
     */
    val gradleSoftwareComponentFuture: Future<AdhocComponentWithVariants> = project.lazyFuture {
        val softwareComponentFactory = (target.project as ProjectInternal).services.get(SoftwareComponentFactory::class.java)
        val adhocSoftwareComponent = softwareComponentFactory.adhoc(target.targetName)

        adhocSoftwareComponent.also {
            target.applyUserDefinedAttributesJob.await()
            kotlinUsagesFuture.await().forEach { kotlinUsage ->
                val configuration = target.project.configurations.getByName(kotlinUsage.dependencyConfigurationName)
                val mavenScope = kotlinUsage.mavenScope
                adhocSoftwareComponent.addVariantsFromConfiguration(configuration) { details ->
                    if (mavenScope != null) {
                        details.mapToMavenScope(mavenScope.name.toLowerCaseAsciiOnly())
                    } else {
                        details.mapToOptional()
                    }
                }
            }
        }
    }
}
