/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.toolchain

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributesSchema
import org.jetbrains.kotlin.gradle.plugin.KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable
import org.jetbrains.kotlin.gradle.utils.setAttribute

/**
 * This class provides functionality for setting up attributes matching strategy and
 * transformation for a Kotlin Native Bundle configurations.
 *
 * @property attribute The attribute object representing the Kotlin Native Bundle type.
 */
internal object KotlinNativeBundleArtifactFormat {

    val attribute: Attribute<KotlinNativeBundleArtifactsTypes> =
        Attribute.of("kotlin.native.bundle.type", KotlinNativeBundleArtifactsTypes::class.java)

    internal enum class KotlinNativeBundleArtifactsTypes {
        ARCHIVE,
        DIRECTORY
    }

    /**
     * Sets up the attributes matching strategy for the given attributes schema.
     *
     * @param attributesSchema The attributes schema to set up the matching strategy for.
     */
    internal fun setupAttributesMatchingStrategy(attributesSchema: AttributesSchema) {
        attributesSchema.attribute(attribute)
    }

    /**
     * Sets up the necessary transformations for handling artifact types "tar.gz" and "zip" in the given project.
     *
     * @param project The project in which to set up the transformations.
     */
    internal fun setupTransform(project: Project) {
        project.dependencies.artifactTypes.maybeCreate("tar.gz").also { artifactType ->
            artifactType.attributes.setAttribute(attribute, KotlinNativeBundleArtifactsTypes.ARCHIVE)
        }

        project.dependencies.artifactTypes.maybeCreate("zip").also { artifactType ->
            artifactType.attributes.setAttribute(attribute, KotlinNativeBundleArtifactsTypes.ARCHIVE)
        }

        project.dependencies.registerTransform(UnzipTransformationAction::class.java) { transform ->
            transform.from.setAttribute(attribute, KotlinNativeBundleArtifactsTypes.ARCHIVE)
            transform.to.setAttribute(attribute, KotlinNativeBundleArtifactsTypes.DIRECTORY)
        }
    }

    internal fun addKotlinNativeBundleConfiguration(project: Project) {
        project.configurations
            .maybeCreateResolvable(KOTLIN_NATIVE_BUNDLE_CONFIGURATION_NAME) {
                defaultDependencies {
                    it.add(project.dependencies.create(NativeCompilerDownloader.getCompilerDependencyNotation(project)))
                }
                if (!attributes.contains(attribute)) {
                    attributes.setAttribute(attribute, KotlinNativeBundleArtifactsTypes.DIRECTORY)
                }
            }
    }
}