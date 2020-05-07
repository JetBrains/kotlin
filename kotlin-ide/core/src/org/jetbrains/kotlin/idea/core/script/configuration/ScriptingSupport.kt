/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.utils.ScriptClassRootsCache
import org.jetbrains.kotlin.psi.KtFile

abstract class ScriptingSupport {
    abstract class Provider {
        abstract fun isApplicable(file: VirtualFile): Boolean
        abstract fun isConfigurationLoadingInProgress(file: KtFile): Boolean
        abstract fun collectConfigurations(builder: ScriptClassRootsCache.Builder)

        companion object {
            val EPN: ExtensionPointName<Provider> =
                ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.scriptingSupportProvider")
        }
    }
}
