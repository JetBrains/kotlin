/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.PathMacros
import org.jetbrains.kotlin.utils.PathUtil.kotlinPathsForIdeaPlugin

/**
 * Some actions have to be performed before loading and opening any project.
 *
 * E.g. path variables have to be registered in advance as modules could rely on some path variables.
 */
class PluginStartupListener : ApplicationInitializedListener {

    override fun componentsInitialized() {
        registerPathVariable()
    }

    private fun registerPathVariable() {
        val macros = PathMacros.getInstance()
        macros.setMacro(KOTLIN_BUNDLED_PATH_VARIABLE, kotlinPathsForIdeaPlugin.homePath.path)
    }

    companion object {
        const val KOTLIN_BUNDLED_PATH_VARIABLE = "KOTLIN_BUNDLED"
    }
}