/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.extras.KlibExtra
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId
import org.jetbrains.kotlin.library.*


internal fun IdeaKotlinProjectCoordinates(identifier: ProjectComponentIdentifier): IdeaKotlinProjectCoordinates {
    return IdeaKotlinProjectCoordinates(
        buildId = identifier.build.name,
        projectPath = identifier.projectPath,
        projectName = identifier.projectName
    )
}

internal fun IdeaKotlinProjectCoordinates(project: Project): IdeaKotlinProjectCoordinates {
    return IdeaKotlinProjectCoordinates(
        buildId = project.currentBuildId().name,
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

internal fun IdeaKotlinBinaryCoordinates(identifier: ModuleComponentIdentifier): IdeaKotlinBinaryCoordinates {
    return IdeaKotlinBinaryCoordinates(
        group = identifier.group,
        module = identifier.module,
        version = identifier.version
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
