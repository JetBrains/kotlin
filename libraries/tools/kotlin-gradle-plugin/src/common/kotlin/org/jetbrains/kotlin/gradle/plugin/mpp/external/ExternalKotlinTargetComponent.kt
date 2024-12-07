/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.external

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.COMPILE
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsageContext.MavenScope.RUNTIME
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinTargetComponent.TargetProvider
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class ExternalKotlinTargetComponent(
    val targetProvider: TargetProvider,
) : InternalKotlinTargetComponent(), KotlinTargetComponentWithPublication, ComponentWithCoordinates {

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

    private val _usages: Set<DefaultKotlinUsageContext> by lazy {
        val compilation = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            ?: error("Missing conventional '${KotlinCompilation.MAIN_COMPILATION_NAME}' compilation in '$target'")

        setOf(
            DefaultKotlinUsageContext(compilation, COMPILE, target.apiElementsPublishedConfiguration.name),
            DefaultKotlinUsageContext(compilation, RUNTIME, target.runtimeElementsPublishedConfiguration.name),
            DefaultKotlinUsageContext(
                compilation = compilation,
                mavenScope = null,
                dependencyConfigurationName = target.sourcesElementsPublishedConfiguration.name,
                includeIntoProjectStructureMetadata = false,
                publishOnlyIf = { target.isSourcesPublishable }
            )
        )
    }

    override fun getUsages(): Set<KotlinUsageContext> = _usages.filter { it.publishOnlyIf.predicate() }.toSet()
}
