/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.*
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.capabilities.Capability
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.utils.buildPathCompat
import org.jetbrains.kotlin.project.model.*



private fun ModuleComponentIdentifier.toSingleKpmModuleIdentifier(classifier: String? = null): KpmMavenModuleIdentifier =
    KpmMavenModuleIdentifier(moduleIdentifier.group, moduleIdentifier.name, classifier)

internal fun ComponentIdentifier.matchesModule(module: KpmModule): Boolean =
    matchesModuleIdentifier(module.moduleIdentifier)

internal fun ResolvedComponentResult.toKpmModuleIdentifiers(): List<KpmModuleIdentifier> {
    val classifiers = moduleClassifiersFromCapabilities(variants.flatMap { it.capabilities })
    return classifiers.map { moduleClassifier -> toKpmModuleIdentifier(moduleClassifier) }
}

internal fun ResolvedVariantResult.toKpmModuleIdentifiers(): List<KpmModuleIdentifier> {
    val classifiers = moduleClassifiersFromCapabilities(capabilities)
    return classifiers.map { moduleClassifier -> toKpmModuleIdentifier(moduleClassifier) }
}

// FIXME this mapping doesn't have enough information to choose auxiliary modules
internal fun ResolvedComponentResult.toSingleKpmModuleIdentifier(): KpmModuleIdentifier {
    val classifiers = moduleClassifiersFromCapabilities(variants.flatMap { it.capabilities })
    val moduleClassifier = classifiers.single() // FIXME handle multiple capabilities
    return toKpmModuleIdentifier(moduleClassifier)
}

internal fun ResolvedVariantResult.toSingleKpmModuleIdentifier(): KpmModuleIdentifier = toKpmModuleIdentifiers().singleOrNull()
    ?: error("Unexpected amount of KPM Identifiers from '$this'. Only single Module Identifier was expected")

private fun ResolvedComponentResult.toKpmModuleIdentifier(moduleClassifier: String?): KpmModuleIdentifier {
    return when (val id = id) {
        is ProjectComponentIdentifier -> KpmLocalModuleIdentifier(id.build.buildPathCompat, id.projectPath, moduleClassifier)
        is ModuleComponentIdentifier -> id.toSingleKpmModuleIdentifier()
        else -> KpmMavenModuleIdentifier(moduleVersion?.group.orEmpty(), moduleVersion?.name.orEmpty(), moduleClassifier)
    }
}

private fun ResolvedVariantResult.toKpmModuleIdentifier(moduleClassifier: String?): KpmModuleIdentifier {
    return when (val id = owner) {
        is ProjectComponentIdentifier -> KpmLocalModuleIdentifier(id.build.buildPathCompat, id.projectPath, moduleClassifier)
        is ModuleComponentIdentifier -> id.toSingleKpmModuleIdentifier()
        else -> error("Unexpected component identifier '$id' of type ${id.javaClass}")
    }
}

internal fun moduleClassifiersFromCapabilities(capabilities: Iterable<Capability>): Iterable<String?> {
    val classifierCapabilities = capabilities.filter { it.name.contains("..") }
    return if (classifierCapabilities.none()) listOf(null) else classifierCapabilities.map { it.name.substringAfterLast("..") /*FIXME invent a more stable scheme*/ }
}

internal fun ComponentIdentifier.matchesModuleIdentifier(id: KpmModuleIdentifier): Boolean =
    when (id) {
        is KpmLocalModuleIdentifier -> {
            val projectId = this as? ProjectComponentIdentifier
            projectId?.build?.buildPathCompat == id.buildId && projectId.projectPath == id.projectId
        }
        is KpmMavenModuleIdentifier -> {
            val componentId = this as? ModuleComponentIdentifier
            componentId?.toSingleKpmModuleIdentifier() == id
        }
        else -> false
    }
