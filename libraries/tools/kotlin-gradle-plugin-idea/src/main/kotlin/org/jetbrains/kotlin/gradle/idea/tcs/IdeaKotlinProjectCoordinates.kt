/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import java.io.Serializable

data class IdeaKotlinProjectCoordinates(
    val buildId: String,
    val projectPath: String,
    val projectName: String
) : Serializable {
    override fun toString(): String {
        return "${buildId.takeIf { it != ":" }?.plus(":").orEmpty()}$projectPath"
    }

    internal companion object {
        const val serialVersionUID = 0L
    }
}
