/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.MavenPublicationCoordinatesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.ComputedCapability

class GradleKpmDefaultSingleMavenPublishedModuleHolder(
    private var module: GradleKpmModule, override val defaultPublishedModuleSuffix: String?
) : GradleKpmSingleMavenPublishedModuleHolder {
    private val project get() = module.project

    private var assignedMavenPublication: MavenPublication? = null

    private val publicationAssignedHandlers = mutableListOf<(MavenPublication) -> Unit>()

    override fun assignMavenPublication(publication: MavenPublication) {
        if (assignedMavenPublication != null) error("already assigned publication $publication")
        assignedMavenPublication = publication
        publicationAssignedHandlers.forEach { it(publication) }
    }

    override fun whenPublicationAssigned(handlePublication: (MavenPublication) -> Unit) {
        assignedMavenPublication?.let(handlePublication) ?: publicationAssignedHandlers.add(handlePublication)
    }

    override val publishedMavenModuleCoordinates: PublishedModuleCoordinatesProvider = MavenPublicationCoordinatesProvider(
        project,
        { assignedMavenPublication },
        defaultPublishedModuleSuffix,
        capabilities = listOfNotNull(ComputedCapability.capabilityStringFromModule(module))
    )
}
