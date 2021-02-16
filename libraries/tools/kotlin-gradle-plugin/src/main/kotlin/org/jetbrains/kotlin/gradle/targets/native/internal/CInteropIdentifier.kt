/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.tasks.Input

/**
 * Project unique identifier for all cinterops
 */
data class CInteropIdentifier internal constructor(
    val compilationTaskName: String,
    val cinteropName: String
) {
    @get:Input
    val uniqueName: String = "cinterop:$compilationTaskName/$cinteropName"
    override fun toString(): String = uniqueName
}
