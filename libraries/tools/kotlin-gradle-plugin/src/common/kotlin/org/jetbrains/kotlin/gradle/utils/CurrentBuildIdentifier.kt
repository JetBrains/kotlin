/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.component.BuildIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.currentBuildId


internal val Project.currentBuild: CurrentBuildIdentifier
    get() = extraProperties.getOrPut("org.jetbrains.kotlin.gradle.utils.currentBuild") { CurrentBuildIdentifierImpl(this.currentBuildId()) }

/**
 * Utility that can be used to test if a certain project or [ComponentIdentifier] belongs
 * to the associated Gradle build in a composite build setup
 */
internal interface CurrentBuildIdentifier {
    operator fun contains(project: Project): Boolean
    operator fun contains(id: ComponentIdentifier): Boolean
}

internal operator fun CurrentBuildIdentifier.contains(component: ResolvedComponentResult): Boolean {
    return component.id in this
}

/* Implementation */

private class CurrentBuildIdentifierImpl(private val currentBuildIdentifier: BuildIdentifier) : CurrentBuildIdentifier {
    override fun contains(project: Project): Boolean {
        return project.currentBuildId() == currentBuildIdentifier
    }

    override fun contains(id: ComponentIdentifier): Boolean {
        return id.buildOrNull == currentBuildIdentifier
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurrentBuildIdentifierImpl) return false
        return this.currentBuildIdentifier == other.currentBuildIdentifier
    }

    override fun hashCode(): Int {
        return currentBuildIdentifier.hashCode()
    }
}
