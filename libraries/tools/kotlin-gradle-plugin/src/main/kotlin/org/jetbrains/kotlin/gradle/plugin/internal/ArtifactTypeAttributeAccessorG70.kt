/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.attributes.Attribute
import org.gradle.api.internal.artifacts.ArtifactAttributes

internal class ArtifactTypeAttributeAccessorG70(
) : ArtifactTypeAttributeAccessor {
    override val artifactTypeAttribute: Attribute<String>
        get() = ArtifactAttributes.ARTIFACT_FORMAT

    internal class ArtifactTypeAttributeAccessorVariantFactoryG70 :
        ArtifactTypeAttributeAccessor.ArtifactTypeAttributeAccessorVariantFactory {
        override fun getInstance(): ArtifactTypeAttributeAccessor = ArtifactTypeAttributeAccessorG70()
    }
}