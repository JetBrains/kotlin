/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.component.BuildIdentifier
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

internal interface BuildIdentifierAccessor {
    /**
     * Will return the [BuildIdentifier.getName] for older Gradle versions,
     * and will calculate the 'buildName' from the new 'buildPath' for Gradle versions higher than 8.2
     */
    val buildName: String

    /**
     * Will return [BuildIdentifier.getBuildPath] for Gradle versions higher than 8.2
     * Will calculate the build path from the previously accessible [BuildIdentifier.getName]:
     * Note, this calculation will not be correct for nested composite builds!
     */
    val buildPath: String

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(buildIdentifier: BuildIdentifier): BuildIdentifierAccessor
    }
}

internal class DefaultBuildIdentifierAccessor(private val buildIdentifier: BuildIdentifier) : BuildIdentifierAccessor {

    override val buildName: String
        get() = if (buildIdentifier.buildPath == ":") ":" else buildIdentifier.buildPath.split(":").last()

    override val buildPath: String
        get() = buildIdentifier.buildPath

    internal class Factory : BuildIdentifierAccessor.Factory {
        override fun getInstance(buildIdentifier: BuildIdentifier): BuildIdentifierAccessor {
            return DefaultBuildIdentifierAccessor(buildIdentifier)
        }
    }
}

internal fun BuildIdentifier.compatAccessor(project: Project): BuildIdentifierAccessor = project
    .variantImplementationFactory<BuildIdentifierAccessor.Factory>()
    .getInstance(this)

internal fun BuildIdentifier.compatAccessor(variantImplementationFactory: Provider<BuildIdentifierAccessor.Factory>) =
    variantImplementationFactory.get().getInstance(this)