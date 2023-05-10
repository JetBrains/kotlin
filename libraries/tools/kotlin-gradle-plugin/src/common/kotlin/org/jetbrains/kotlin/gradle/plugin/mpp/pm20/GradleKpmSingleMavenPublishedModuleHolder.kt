/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.publish.maven.MavenPublication

interface GradleKpmSingleMavenPublishedModuleHolder {
    fun assignMavenPublication(publication: MavenPublication)
    fun whenPublicationAssigned(handlePublication: (MavenPublication) -> Unit)
    val defaultPublishedModuleSuffix: String?
    val publishedMavenModuleCoordinates: PublishedModuleCoordinatesProvider
}
