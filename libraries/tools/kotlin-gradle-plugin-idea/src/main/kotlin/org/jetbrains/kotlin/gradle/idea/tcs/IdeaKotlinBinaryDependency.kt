/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf

sealed class IdeaKotlinBinaryDependency : IdeaKotlinDependency {
    abstract override val coordinates: IdeaKotlinBinaryCoordinates?

    companion object {
        const val KOTLIN_COMPILE_BINARY_TYPE = "KOTLIN_COMPILE"
    }
}

data class IdeaKotlinResolvedBinaryDependency(
    val binaryType: String,
    val classpath: IdeaKotlinClasspath,
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

val IdeaKotlinResolvedBinaryDependency.isKotlinCompileBinaryType
    get() = this.binaryType == IdeaKotlinBinaryDependency.KOTLIN_COMPILE_BINARY_TYPE
