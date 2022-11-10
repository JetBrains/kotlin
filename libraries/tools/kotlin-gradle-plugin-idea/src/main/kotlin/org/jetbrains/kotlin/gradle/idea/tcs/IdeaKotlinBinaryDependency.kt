/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.MutableExtras
import java.io.File

sealed class IdeaKotlinBinaryDependency : IdeaKotlinDependency {
    abstract override val coordinates: IdeaKotlinBinaryCoordinates?
}

data class IdeaKotlinResolvedBinaryDependency(
    val binaryType: String,
    val binaryFile: File,
    override val extras: MutableExtras,
    override val coordinates: IdeaKotlinBinaryCoordinates?
) : IdeaKotlinBinaryDependency() {
    internal companion object {
        const val serialVersionUID = 0L
    }
}

data class IdeaKotlinUnresolvedBinaryDependency(
    val cause: String?,
    override val coordinates: IdeaKotlinBinaryCoordinates?,
    override val extras: MutableExtras
) : IdeaKotlinBinaryDependency() {
    internal companion object {
        const val serialVersionUID = 0L
    }
}