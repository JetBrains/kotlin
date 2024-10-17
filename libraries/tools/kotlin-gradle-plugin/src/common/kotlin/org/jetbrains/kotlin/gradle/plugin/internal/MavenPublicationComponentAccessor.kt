/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.logging.Logger
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * Queries [SoftwareComponent] from [MavenPublication]
 *
 * WARNING: Try to avoid using this API as it uses internal Gradle API
 */
internal interface MavenPublicationComponentAccessor {
    fun getComponentOrNull(publication: MavenPublication): SoftwareComponent?

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(project: Project): MavenPublicationComponentAccessor
    }
}

internal class DefaultMavenPublicationComponentAccessorFactory :
    MavenPublicationComponentAccessor.Factory {
    override fun getInstance(project: Project) = DefaultMavenPublicationComponentAccessor(project.logger)
}

internal class DefaultMavenPublicationComponentAccessor(private val logger: Logger): MavenPublicationComponentAccessor {
    override fun getComponentOrNull(publication: MavenPublication): SoftwareComponent? {
        if (publication !is MavenPublicationInternal) return null
        // Wrap in runCatching to avoid potential
        val componentResult = runCatching { publication.component.orNull }
        if (componentResult.isFailure) {
            logger.warn("Can't get component for $publication", componentResult.exceptionOrNull()!!)
            return null
        }

        return componentResult.getOrNull()
    }
}

internal fun MavenPublication.getComponentOrNull(project: Project): SoftwareComponent? = project
    .variantImplementationFactory<MavenPublicationComponentAccessor.Factory>()
    .getInstance(project)
    .getComponentOrNull(this)