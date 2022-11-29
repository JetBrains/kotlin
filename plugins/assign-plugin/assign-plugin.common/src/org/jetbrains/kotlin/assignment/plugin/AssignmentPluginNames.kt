/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin

import org.jetbrains.kotlin.name.Name

object AssignmentPluginNames {
    const val PLUGIN_ID = "org.jetbrains.kotlin.assignment"
    const val ANNOTATION_OPTION_NAME = "annotation"
    val ASSIGN_METHOD = Name.identifier("assign")
}
