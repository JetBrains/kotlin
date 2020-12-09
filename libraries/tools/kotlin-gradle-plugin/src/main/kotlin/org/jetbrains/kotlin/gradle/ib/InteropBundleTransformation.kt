/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ib

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.internal.artifacts.transform.UnzipTransform
import org.jetbrains.kotlin.commonizer.api.*
import org.jetbrains.kotlin.compilerRunner.konanHome
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File


internal val COMMONIZER_TARGET_ATTRIBUTE = Attribute.of("commonizer-target", String::class.java)
internal const val INTEROP_BUNDLE_COMMONIZIER_TARGET = "*interob-bundle*"
internal const val WILDCARD_COMMONIIZER_TARGET = "*"


internal val Project.isInteropBundleTransformationEnabled: Boolean
    get() = PropertiesProvider(this).enableInteropBundleTransformation == true

internal fun Project.setupInteropBundleTransformationIfEnabled() {
    if (!isInteropBundleTransformationEnabled) return
    setupTransformations()
}

private fun Project.setupTransformations() = dependencies.run {
    setupAttributeSchema()
    registerUnzipInteropBundleCommonizerTransformation()
    registerInteropBundleCommonizerTransformation()
    registerInteropBundlePlatformSelectionTransformation()
    registerCommonizerOutputSelectionTransformation()
}

private fun Project.setupAttributeSchema() {
    val kotlin = multiplatformExtensionOrNull ?: return
    dependencies.attributesSchema.attribute(COMMONIZER_TARGET_ATTRIBUTE)

    dependencies.artifactTypes.register(ZIPPED_INTEROP_BUNDLE_FILE_EXTENSION) { definition ->
        definition.attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, ZIPPED_INTEROP_BUNDLE_ARTIFACT_TYPE)
        definition.attributes.attribute(COMMONIZER_TARGET_ATTRIBUTE, INTEROP_BUNDLE_COMMONIZIER_TARGET)
    }

    kotlin.targets.all { target ->
        (target.compilations as DomainObjectCollection<*>).all { compilation ->
            if (compilation is KotlinCompilation<*>) {
                setupAttributeSchema(compilation)
            }
        }
    }

    kotlin.sourceSets.all { sourceSet ->
        setupAttributeSchema(sourceSet)
    }
}

private fun Project.setupAttributeSchema(compilation: KotlinCompilation<*>) {
    val commonizerTarget = getCommonizerTarget(compilation) ?: return
    val configurationsNames = compilation.relatedConfigurationNames.toSet()
    val configurations = configurationsNames.mapNotNull { name -> configurations.findByName(name) }
    configurations.forEach { configuration -> configuration.setCommonizerTargetAttributeIfAbsent(commonizerTarget) }
}


private fun Project.setupAttributeSchema(sourceSet: KotlinSourceSet) {
    val commonizerTarget = getCommonizerTarget(sourceSet) ?: return
    val configurationsNames = sourceSet.relatedConfigurationNames.toSet()
    val configurations = configurationsNames.mapNotNull { name -> configurations.findByName(name) }
    configurations.forEach { configuration -> configuration.setCommonizerTargetAttributeIfAbsent(commonizerTarget) }
}

private fun Project.registerUnzipInteropBundleCommonizerTransformation() = dependencies.run {
    registerTransform(UnzipTransform::class.java) { spec ->
        spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, ZIPPED_INTEROP_BUNDLE_ARTIFACT_TYPE)
        spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, INTEROP_BUNDLE_ARTIFACT_TYPE)
    }
}

private fun Project.registerInteropBundleCommonizerTransformation() = dependencies.run {
    registerTransform(InteropBundleCommonizerTransformation::class.java) { spec ->
        spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, INTEROP_BUNDLE_ARTIFACT_TYPE)
        spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, COMMONIZED_INTEROP_BUNDLE_ARTIFACT_TYPE)

        spec.from.attribute(COMMONIZER_TARGET_ATTRIBUTE, INTEROP_BUNDLE_COMMONIZIER_TARGET)
        spec.to.attribute(COMMONIZER_TARGET_ATTRIBUTE, WILDCARD_COMMONIIZER_TARGET)

        spec.parameters { parameters ->
            parameters.konanHome = File(project.konanHome).absoluteFile
            parameters.commonizerClasspath = configurations.getByName(KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME).resolve()
            parameters.outputHierarchy = project.getCommonizerOutputHierarchy()
        }
    }
}

private fun Project.registerCommonizerOutputSelectionTransformation() = dependencies.run {
    for (sharedCommonizerTarget in getAllSharedCommonizerTargets()) {
        registerTransform(CommonizerOutputSelectionTransformation::class.java) { spec ->

            spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, COMMONIZED_INTEROP_BUNDLE_ARTIFACT_TYPE)
            spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, KLIB_ARTIFACT_TYPE)

            spec.from.attribute(COMMONIZER_TARGET_ATTRIBUTE, WILDCARD_COMMONIIZER_TARGET)
            spec.to.attribute(COMMONIZER_TARGET_ATTRIBUTE, sharedCommonizerTarget.identityString)

            spec.parameters { parameters ->
                parameters.target = sharedCommonizerTarget
            }
        }
    }
}


private fun Project.registerInteropBundlePlatformSelectionTransformation() = dependencies.run {
    for ((_, konanTarget) in KonanTarget.predefinedTargets) {
        registerTransform(InteropBundlePlatformSelectionTransformation::class.java) { spec ->

            spec.from.attribute(ARTIFACT_TYPE_ATTRIBUTE, INTEROP_BUNDLE_ARTIFACT_TYPE)
            spec.to.attribute(ARTIFACT_TYPE_ATTRIBUTE, KLIB_ARTIFACT_TYPE)

            spec.from.attribute(COMMONIZER_TARGET_ATTRIBUTE, INTEROP_BUNDLE_COMMONIZIER_TARGET)
            spec.to.attribute(COMMONIZER_TARGET_ATTRIBUTE, CommonizerTarget(konanTarget).identityString)

            spec.parameters { parameters ->
                parameters.target = LeafCommonizerTarget(konanTarget)
            }
        }
    }
}


private fun Project.getAllSharedCommonizerTargets(): Set<SharedCommonizerTarget> {
    val kotlin = multiplatformExtensionOrNull ?: return emptySet()
    return kotlin.sourceSets
        .map { sourceSet -> getCommonizerTarget(sourceSet) }
        .filterIsInstance<SharedCommonizerTarget>()
        .toSet()

}

private fun Configuration.setCommonizerTargetAttributeIfAbsent(target: CommonizerTarget) {
    setCommonizerTargetAttributeIfAbsent(target.identityString)
}

private fun Configuration.setCommonizerTargetAttributeIfAbsent(value: String) {
    if (!attributes.contains(COMMONIZER_TARGET_ATTRIBUTE)) {
        attributes.attribute(COMMONIZER_TARGET_ATTRIBUTE, value)
    }
}

private fun Project.getCommonizerOutputHierarchy(): SharedCommonizerTarget? {
    return getAllSharedCommonizerTargets().maxBy { it.order }?.also { target ->
        require(target.order <= 1) { "Commonizer only supports one level of hierarchy at the moment" }
    }
}
