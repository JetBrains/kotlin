/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.Extras
import java.io.File

sealed class IdeaKotlinBinaryDependency : IdeaKotlinDependency {
    abstract override val coordinates: IdeaKotlinBinaryCoordinates?
}

data class IdeaKotlinResolvedBinaryDependency(
    val binaryType: String,
    val binaryFile: File,
    override val extras: Extras,
    override val coordinates: IdeaKotlinBinaryCoordinates?
) : IdeaKotlinBinaryDependency()

data class IdeaKotlinUnresolvedBinaryDependency(
    val cause: String?,
    override val coordinates: IdeaKotlinBinaryCoordinates?,
    override val extras: Extras
) : IdeaKotlinBinaryDependency()