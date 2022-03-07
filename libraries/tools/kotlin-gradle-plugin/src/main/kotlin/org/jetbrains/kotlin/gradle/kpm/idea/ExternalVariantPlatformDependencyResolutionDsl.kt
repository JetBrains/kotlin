/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinPlatformDependencyResolver.ArtifactResolution
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinProjectModelBuilder.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.FragmentAttributes

@DslMarker
@ExternalVariantApi
annotation class ExternalVariantPlatformDependencyResolutionDsl

@ExternalVariantApi
fun KotlinPm20ProjectExtension.configureIdeaKotlinSpecialPlatformDependencyResolution(
    configure: IdeaKotlinPlatformDependencyResolutionDslHandle.() -> Unit
) {
    IdeaKotlinPlatformDependencyResolutionDslHandle(ideaKotlinProjectModelBuilder).also(configure).setup()
}

@ExternalVariantPlatformDependencyResolutionDsl
class IdeaKotlinPlatformDependencyResolutionDslHandle internal constructor(
    private val toolingModelBuilder: IdeaKotlinProjectModelBuilder,
    private val constraint: FragmentConstraint = FragmentConstraint.unconstrained,
    private val parent: IdeaKotlinPlatformDependencyResolutionDslHandle? = null
) {
    private val artifactViewDslHandles = mutableListOf<ArtifactViewDslHandle>()
    private val children = mutableListOf<IdeaKotlinPlatformDependencyResolutionDslHandle>()
    private val additionalDependencies = mutableListOf<IdeaKotlinDependencyResolver>()

    @ExternalVariantPlatformDependencyResolutionDsl
    var platformResolutionAttributes: FragmentAttributes<KotlinGradleFragment>? = null

    @ExternalVariantPlatformDependencyResolutionDsl
    class ArtifactViewDslHandle(
        @property:ExternalVariantPlatformDependencyResolutionDsl
        var binaryType: String
    ) {
        @ExternalVariantPlatformDependencyResolutionDsl
        var attributes: FragmentAttributes<KotlinGradleFragment> = FragmentAttributes { }

        @ExternalVariantPlatformDependencyResolutionDsl
        fun attributes(setAttributes: KotlinGradleFragmentConfigurationAttributesContext<KotlinGradleFragment>.() -> Unit) {
            attributes += FragmentAttributes(setAttributes)
        }
    }

    @ExternalVariantPlatformDependencyResolutionDsl
    fun withConstraint(
        constraint: FragmentConstraint,
        configure: IdeaKotlinPlatformDependencyResolutionDslHandle.() -> Unit
    ) {
        children += IdeaKotlinPlatformDependencyResolutionDslHandle(
            toolingModelBuilder, constraint, this,
        ).apply(configure)
    }


    @ExternalVariantPlatformDependencyResolutionDsl
    fun withPlatformResolutionAttributes(
        setAttributes: KotlinGradleFragmentConfigurationAttributesContext<KotlinGradleFragment>.() -> Unit
    ) {
        val additionalAttributes = FragmentAttributes(setAttributes)
        this.platformResolutionAttributes = platformResolutionAttributes?.plus(additionalAttributes) ?: additionalAttributes
    }

    @ExternalVariantPlatformDependencyResolutionDsl
    fun artifactView(binaryType: String, configure: ArtifactViewDslHandle.() -> Unit = {}) {
        artifactViewDslHandles += ArtifactViewDslHandle(binaryType).apply(configure)
    }

    @ExternalVariantPlatformDependencyResolutionDsl
    fun additionalDependencies(dependencyProvider: (KotlinGradleFragment) -> List<IdeaKotlinDependency>) {
        this.additionalDependencies += IdeaKotlinDependencyResolver { fragment -> dependencyProvider(fragment).toSet() }
    }

    internal fun setup() {
        children.forEach { child -> child.setup() }

        val constraint = buildConstraint()
        val platformResolutionAttributes = buildPlatformResolutionAttributes()

        /* Setup artifact views */
        artifactViewDslHandles.toList().forEach { artifactViewDslHandle ->
            toolingModelBuilder.registerDependencyResolver(
                IdeaKotlinPlatformDependencyResolver(
                    binaryType = artifactViewDslHandle.binaryType,
                    artifactResolution = ArtifactResolution.Variant(artifactViewDslHandle.attributes)
                ),
                constraint = FragmentConstraint.isVariant and constraint,
                phase = DependencyResolutionPhase.BinaryDependencyResolution,
                level = DependencyResolutionLevel.Overwrite
            )

            if (platformResolutionAttributes != null) {
                toolingModelBuilder.registerDependencyResolver(
                    IdeaKotlinPlatformDependencyResolver(
                        binaryType = artifactViewDslHandle.binaryType,
                        artifactResolution = ArtifactResolution.PlatformFragment(
                            artifactViewAttributes = artifactViewDslHandle.attributes,
                            platformResolutionAttributes = platformResolutionAttributes
                        )
                    ),
                    constraint = !FragmentConstraint.isVariant and constraint,
                    phase = DependencyResolutionPhase.BinaryDependencyResolution,
                    level = DependencyResolutionLevel.Overwrite
                )
            }
        }

        /* Setup plain additional resolvers */
        additionalDependencies.toList().forEach { additionalDependencyResolver ->
            toolingModelBuilder.registerDependencyResolver(
                resolver = additionalDependencyResolver,
                constraint = constraint,
                phase = DependencyResolutionPhase.BinaryDependencyResolution,
                level = DependencyResolutionLevel.Overwrite
            )
        }
    }

    private fun buildConstraint(): FragmentConstraint {
        val parentConstraint = parent?.buildConstraint() ?: return constraint
        return parentConstraint and this.constraint
    }

    private fun buildPlatformResolutionAttributes(): FragmentAttributes<KotlinGradleFragment>? {
        val parentAttributes = parent?.buildPlatformResolutionAttributes() ?: return this.platformResolutionAttributes
        return parentAttributes + (this.platformResolutionAttributes ?: return parentAttributes)
    }
}
