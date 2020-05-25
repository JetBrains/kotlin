/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsCache
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener
import org.jetbrains.kotlin.idea.core.script.ucache.ScriptClassRootsBuilder
import org.jetbrains.kotlin.psi.KtFile

/**
 * Extension point for overriding default Kotlin scripting support.
 *
 * Implementation should store script configuration internally (in memory and/or fs),
 * and provide it inside [collectConfigurations] using the [ScriptClassRootsCache.LightScriptInfo].
 * Custom data can be attached to [ScriptClassRootsCache.LightScriptInfo] and retrieved
 * by calling [ScriptClassRootsCache.getLightScriptInfo].
 *
 * [ScriptChangeListener] can be used to listen script changes.
 * [CompositeScriptConfigurationManager.updater] should be used to schedule configuration reloading.
 *
 * [isApplicable] should return true for files that is covered by that support.
 *
 * [isConfigurationLoadingInProgress] is used to pause analyzing.
 *
 * Long read: [idea/idea-gradle/src/org/jetbrains/kotlin/idea/scripting/gradle/README.md].
 *
 * @sample GradleBuildRootsManager
 */
abstract class ScriptingSupport {
    abstract fun isApplicable(file: VirtualFile): Boolean
    abstract fun isConfigurationLoadingInProgress(file: KtFile): Boolean
    abstract fun collectConfigurations(builder: ScriptClassRootsBuilder)

    companion object {
        val EPN: ExtensionPointName<ScriptingSupport> =
            ExtensionPointName.create("org.jetbrains.kotlin.scripting.idea.scriptingSupport")
    }
}
