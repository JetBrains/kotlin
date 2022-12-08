/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import java.io.File

sealed class IdeaKotlinBinaryDependency : IdeaKotlinDependency {
    abstract override val coordinates: IdeaKotlinBinaryCoordinates?
}

data class IdeaKotlinResolvedBinaryDependency(
    val binaryType: String,
    val binaryFile: File,
    override val coordinates: IdeaKotlinBinaryCoordinates?,
    override val extras: MutableExtras = mutableExtrasOf()
) : IdeaKotlinBinaryDependency() {
    internal companion object {
        const val serialVersionUID = 0L
    }
}

data class IdeaKotlinUnresolvedBinaryDependency(
    val cause: String?,
    override val coordinates: IdeaKotlinBinaryCoordinates?,
    override val extras: MutableExtras = mutableExtrasOf()
) : IdeaKotlinBinaryDependency() {
    internal companion object {
        const val serialVersionUID = 0L
    }
}

val IdeaKotlinResolvedBinaryDependency.isClasspathBinaryType
    get() = this.binaryType == IdeaKotlinDependency.CLASSPATH_BINARY_TYPE

val IdeaKotlinResolvedBinaryDependency.isSourcesBinaryType
    get() = this.binaryType == IdeaKotlinDependency.SOURCES_BINARY_TYPE

val IdeaKotlinResolvedBinaryDependency.isDocumentationBinaryType
    get() = this.binaryType == IdeaKotlinDependency.DOCUMENTATION_BINARY_TYPE
