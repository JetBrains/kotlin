/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

abstract class ClearBuildStateExtension {
    companion object {
        @JvmStatic
        fun getExtensions(): Array<out ClearBuildStateExtension> {
            return EP_NAME.getExtensions();
        }

        val EP_NAME: ExtensionPointName<ClearBuildStateExtension> =
            ExtensionPointName.create<ClearBuildStateExtension>("org.jetbrains.kotlin.clearBuildState")
    }

    abstract fun clearState(project: Project)
}