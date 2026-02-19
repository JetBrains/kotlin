/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.artifacts.component.BuildIdentifier

internal class BuildIdentifierAccessorG81(
    private val buildIdentifier: BuildIdentifier
) : BuildIdentifierAccessor {

    override val buildName: String
        get() = buildIdentifier.name

    override val buildPath: String
        get() = if (buildIdentifier.name.startsWith(":")) {
            buildIdentifier.name
        } else {
            ":${buildIdentifier.name}"
        }

    internal class Factory : BuildIdentifierAccessor.Factory {
        override fun getInstance(buildIdentifier: BuildIdentifier): BuildIdentifierAccessor {
            return BuildIdentifierAccessorG81(buildIdentifier)
        }
    }
}
