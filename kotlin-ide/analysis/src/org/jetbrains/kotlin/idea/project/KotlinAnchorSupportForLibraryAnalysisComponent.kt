/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.project

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

object KotlinAnchorSupportForLibraryAnalysisComponent {
    private const val anchorSupportOption = "kotlin.use.anchor.services"

    @JvmStatic
    fun setState(project: Project, isEnabled: Boolean) {
        PropertiesComponent.getInstance(project).setValue(anchorSupportOption, isEnabled, false)
    }

    @JvmStatic
    fun isEnabled(project: Project): Boolean =
        PropertiesComponent.getInstance(project).getBoolean(anchorSupportOption)
}

val Project.useAnchorServices: Boolean
    get() = KotlinAnchorSupportForLibraryAnalysisComponent.isEnabled(this)