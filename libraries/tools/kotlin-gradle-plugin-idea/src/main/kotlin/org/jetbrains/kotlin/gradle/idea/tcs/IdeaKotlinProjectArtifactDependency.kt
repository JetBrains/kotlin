/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnusedReceiverParameter")

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.*

data class IdeaKotlinProjectArtifactDependency(
    val type: IdeaKotlinSourceDependency.Type,
    override val coordinates: IdeaKotlinProjectCoordinates,
    override val extras: MutableExtras = mutableExtrasOf()
) : IdeaKotlinDependency {

    fun resolved(sourceSetNames: Iterable<String>): Set<IdeaKotlinSourceDependency> {
        return sourceSetNames.toSet()
            .map { sourceSetName ->
                IdeaKotlinSourceDependency(
                    type = type,
                    extras = extras.toMutableExtras(),
                    coordinates = IdeaKotlinSourceCoordinates(
                        project = coordinates,
                        sourceSetName = sourceSetName
                    )
                )
            }
            .onEach { it.projectArtifactDependencyOrigin = this }
            .toSet()
    }

    internal companion object {
        const val serialVersionUID = 0L

        var IdeaKotlinSourceDependency.projectArtifactDependencyOrigin
                by extrasReadWriteProperty<IdeaKotlinProjectArtifactDependency>("origin")
            private set
    }
}
