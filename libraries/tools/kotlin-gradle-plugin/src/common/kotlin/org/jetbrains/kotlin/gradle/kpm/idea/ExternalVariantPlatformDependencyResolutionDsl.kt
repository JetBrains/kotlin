/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExternalVariantApi::class)

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmPlatformDependencyResolver.ArtifactResolution
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProjectModelBuilder.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmConfigurationAttributesSetup

@DslMarker
@ExternalVariantApi
annotation class ExternalVariantPlatformDependencyResolutionDsl

@ExternalVariantApi
fun KotlinPm20ProjectExtension.configureIdeaKpmSpecialPlatformDependencyResolution(
    configure: IdeaKpmPlatformDependencyResolutionDslHandle.() -> Unit
) {
    IdeaKpmPlatformDependencyResolutionDslHandle(ideaKpmProjectModelBuilder).also(configure).setup()
}

@ExternalVariantPlatformDependencyResolutionDsl
class IdeaKpmPlatformDependencyResolutionDslHandle internal constructor(
    private val toolingModelBuilder: IdeaKpmProjectModelBuilder,
    private val constraint: FragmentConstraint = FragmentConstraint.unconstrained,
    private val parent: IdeaKpmPlatformDependencyResolutionDslHandle? = null
) {
    private val artifactViewDslHandles = mutableListOf<ArtifactViewDslHandle>()
    private val children = mutableListOf<IdeaKpmPlatformDependencyResolutionDslHandle>()
    private val additionalDependencies = mutableListOf<IdeaKpmDependencyResolver>()

    @ExternalVariantPlatformDependencyResolutionDsl
    var platformResolutionAttributes: GradleKpmConfigurationAttributesSetup<GradleKpmFragment>? = null

    @ExternalVariantPlatformDependencyResolutionDsl
    class ArtifactViewDslHandle(
        @property:ExternalVariantPlatformDependencyResolutionDsl
        var binaryType: String
    ) {
        @ExternalVariantPlatformDependencyResolutionDsl
        var attributes: GradleKpmConfigurationAttributesSetup<GradleKpmFragment> = GradleKpmConfigurationAttributesSetup.None

        @ExternalVariantPlatformDependencyResolutionDsl
        fun attributes(setAttributes: GradleKpmConfigurationAttributesSetupContext<GradleKpmFragment>.() -> Unit) {
            attributes += GradleKpmConfigurationAttributesSetup(setAttributes)
        }
    }

    @ExternalVariantPlatformDependencyResolutionDsl
    fun withConstraint(
        constraint: FragmentConstraint,
        configure: IdeaKpmPlatformDependencyResolutionDslHandle.() -> Unit
    ) {
        children += IdeaKpmPlatformDependencyResolutionDslHandle(
            toolingModelBuilder, constraint, this,
        ).apply(configure)
    }


    @ExternalVariantPlatformDependencyResolutionDsl
    fun withPlatformResolutionAttributes(
        setAttributes: GradleKpmConfigurationAttributesSetupContext<GradleKpmFragment>.() -> Unit
    ) {
        val additionalAttributes = GradleKpmConfigurationAttributesSetup(setAttributes)
        this.platformResolutionAttributes = platformResolutionAttributes?.plus(additionalAttributes) ?: additionalAttributes
    }

    @ExternalVariantPlatformDependencyResolutionDsl
    fun artifactView(binaryType: String, configure: ArtifactViewDslHandle.() -> Unit = {}) {
        artifactViewDslHandles += ArtifactViewDslHandle(binaryType).apply(configure)
    }

    @ExternalVariantPlatformDependencyResolutionDsl
    fun additionalDependencies(dependencyProvider: (GradleKpmFragment) -> List<IdeaKpmDependency>) {
        this.additionalDependencies += IdeaKpmDependencyResolver { fragment -> dependencyProvider(fragment).toSet() }
    }

    internal fun setup() {
        children.forEach { child -> child.setup() }

        val constraint = buildConstraint()
        val platformResolutionAttributes = buildPlatformResolutionAttributes()

        /* Setup artifact views */
        artifactViewDslHandles.toList().forEach { artifactViewDslHandle ->
            toolingModelBuilder.registerDependencyResolver(
                IdeaKpmPlatformDependencyResolver(
                    binaryType = artifactViewDslHandle.binaryType,
                    artifactResolution = ArtifactResolution.Variant(artifactViewDslHandle.attributes)
                ),
                constraint = FragmentConstraint.isVariant and constraint,
                phase = DependencyResolutionPhase.BinaryDependencyResolution,
                level = DependencyResolutionLevel.Overwrite
            )

            if (platformResolutionAttributes != null) {
                toolingModelBuilder.registerDependencyResolver(
                    IdeaKpmPlatformDependencyResolver(
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

    private fun buildPlatformResolutionAttributes(): GradleKpmConfigurationAttributesSetup<GradleKpmFragment>? {
        val parentAttributes = parent?.buildPlatformResolutionAttributes() ?: return this.platformResolutionAttributes
        return parentAttributes + (this.platformResolutionAttributes ?: return parentAttributes)
    }
}
