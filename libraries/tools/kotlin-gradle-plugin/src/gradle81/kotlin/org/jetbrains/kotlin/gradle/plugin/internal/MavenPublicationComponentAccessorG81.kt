/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal

internal object MavenPublicationComponentAccessorG81 : MavenPublicationComponentAccessor {
    override fun getComponentOrNull(publication: MavenPublication): SoftwareComponent? {
        if (publication !is MavenPublicationInternal) return null
        return publication.component
    }

    internal class Factory : MavenPublicationComponentAccessor.Factory {
        override fun getInstance(project: Project): MavenPublicationComponentAccessor = MavenPublicationComponentAccessorG81
    }
}