/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.gradle.model.impl

import org.jetbrains.kotlin.gradle.model.Assignment
import java.io.Serializable

/**
 * Implementation of the [ValueContainerAssignment] interface.
 */
data class AssignmentImpl(
    override val name: String,
    override val annotations: List<String>
) : Assignment, Serializable {

    override val modelVersion = serialVersionUID

    companion object {
        private const val serialVersionUID = 1L
    }
}
