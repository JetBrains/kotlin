/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.jetbrains.kotlin.gradle.idea.tcs.*
import org.jetbrains.kotlin.gradle.idea.tcs.extras.KlibExtra
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.utils.currentBuildId
import org.jetbrains.kotlin.gradle.utils.buildNameCompat
import org.jetbrains.kotlin.gradle.utils.buildPathCompat
import org.jetbrains.kotlin.library.*


internal fun IdeaKotlinProjectCoordinates(identifier: ProjectComponentIdentifier): IdeaKotlinProjectCoordinates {
    return IdeaKotlinProjectCoordinates(
        buildPath = identifier.build.buildPathCompat,
        buildName = identifier.build.buildNameCompat,
        projectPath = identifier.projectPath,
        projectName = identifier.projectName
    )
}

internal fun IdeaKotlinProjectCoordinates(project: Project): IdeaKotlinProjectCoordinates {
    val buildIdentifier = project.currentBuildId()
    return IdeaKotlinProjectCoordinates(
        buildPath = buildIdentifier.buildPathCompat,
        buildName = buildIdentifier.buildNameCompat,
        projectPath = project.path,
        projectName = project.name
    )
}



internal fun IdeaKotlinSourceCoordinates(sourceSet: KotlinSourceSet): IdeaKotlinSourceCoordinates {
    return IdeaKotlinSourceCoordinates(
        project = IdeaKotlinProjectCoordinates(sourceSet.project),
        sourceSetName = sourceSet.name
    )
}

internal fun IdeaKotlinBinaryCoordinates(
    identifier: ModuleComponentIdentifier,
    capabilities: List<Capability> = emptyList(),
    attributes: AttributeContainer,
): IdeaKotlinBinaryCoordinates {
    return IdeaKotlinBinaryCoordinates(
        group = identifier.group,
        module = identifier.module,
        version = identifier.version,
        capabilities = capabilities.map(::IdeaKotlinBinaryCapability).toSet(),
        attributes = IdeaKotlinBinaryAttributes(attributes)
    )
}

internal fun IdeaKotlinBinaryCapability(capability: Capability): IdeaKotlinBinaryCapability {
    return IdeaKotlinBinaryCapability(
        group = capability.group,
        name = capability.name,
        version = capability.version
    )
}

internal fun IdeaKotlinBinaryAttributes(attributes: AttributeContainer): IdeaKotlinBinaryAttributes {
    return IdeaKotlinBinaryAttributes(
        attributes.keySet().associate { key -> key.name to attributes.getAttribute(key).toString() }
    )
}

internal fun KlibExtra(library: KotlinLibrary): KlibExtra {
    return KlibExtra(
        builtInsPlatform = library.builtInsPlatform,
        uniqueName = library.uniqueName,
        shortName = library.shortName,
        packageFqName = library.packageFqName,
        nativeTargets = library.nativeTargets,
        commonizerNativeTargets = library.commonizerNativeTargets,
        commonizerTarget = library.commonizerTarget,
        isInterop = library.isInterop
    )
}
