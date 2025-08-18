/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.transform.TransformSpec
import org.gradle.api.artifacts.type.ArtifactTypeDefinition

internal fun <P : TransformParameters, T : TransformAction<P>> DependencyHandler.registerTransformForArtifactType(
    transformClass: Class<T>,
    fromArtifactType: String,
    toArtifactType: String,
    configure: (TransformSpec<P>) -> Unit = {},
) {
    @Suppress("registerTransform_without_artifactType") // this is an actual replacement implementation
    registerTransform(transformClass) { spec ->
        configure(spec)
        spec.from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, fromArtifactType)
        spec.to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, toArtifactType)
    }
}