/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * Gradle 7.3 introduced the artifact type attribute as a part of public API.
 * The old API is now deprecated, so we continue using it only for old Gradle versions
 */
interface ArtifactTypeAttributeAccessor {
    val artifactTypeAttribute: Attribute<String>

    interface ArtifactTypeAttributeAccessorVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(): ArtifactTypeAttributeAccessor
    }
}

internal class DefaultArtifactTypeAttributeAccessorVariantFactory :
    ArtifactTypeAttributeAccessor.ArtifactTypeAttributeAccessorVariantFactory {
    override fun getInstance() = DefaultArtifactTypeAttributeAccessor()
}

internal class DefaultArtifactTypeAttributeAccessor : ArtifactTypeAttributeAccessor {
    override val artifactTypeAttribute: Attribute<String>
        get() = ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
}

internal val Project.artifactTypeAttribute
    get() = variantImplementationFactory<ArtifactTypeAttributeAccessor.ArtifactTypeAttributeAccessorVariantFactory>()
        .getInstance()
        .artifactTypeAttribute