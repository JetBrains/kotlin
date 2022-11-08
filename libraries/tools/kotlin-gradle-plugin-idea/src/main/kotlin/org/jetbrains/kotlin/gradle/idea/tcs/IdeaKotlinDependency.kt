/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.MutableExtras
import java.io.Serializable

sealed interface IdeaKotlinDependency : Serializable {
    val coordinates: IdeaKotlinDependencyCoordinates?
    val extras: MutableExtras

    companion object {
        const val CLASSPATH_BINARY_TYPE = "org.jetbrains.kotlin.gradle.idea.tcs.IdeDependency.classpathBinaryType"
        const val SOURCES_BINARY_TYPE = "org.jetbrains.kotlin.gradle.idea.tcs.IdeDependency.sourcesBinaryType"
        const val DOCUMENTATION_BINARY_TYPE = "org.jetbrains.kotlin.gradle.idea.tcs.IdeDependency.documentationBinaryType"
    }
}
