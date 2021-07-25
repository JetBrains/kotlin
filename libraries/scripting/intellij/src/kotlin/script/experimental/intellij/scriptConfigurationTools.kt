/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.intellij

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiFile

/**
 * Force reloading the script definition associated with the passed [scriptFile] in the Kotlin plugin
 *
 * [updateEditorWithoutNotification] controls whether the update of the indexes and highlighting of the script files
 * based on the reloaded definition should be reloaded automatically or using notification and explicit reload action
 */
fun reloadScriptConfiguration(scriptFile: PsiFile, updateEditorWithoutNotification: Boolean = false) {
    val extensions = scriptFile.project.extensionArea.getExtensionPoint(IdeScriptConfigurationControlFacade.EP_NAME).extensions
    for (extension in extensions) {
        extension.reloadScriptConfiguration(scriptFile, updateEditorWithoutNotification)
    }
}

/**
 * The IntelliJ extension point needed for the [reloadScriptConfiguration] function. Should not be used directly.
 */
interface IdeScriptConfigurationControlFacade {

    fun reloadScriptConfiguration(scriptFile: PsiFile, updateEditorWithoutNotification: Boolean = false)

    companion object {
        val EP_NAME: ExtensionPointName<IdeScriptConfigurationControlFacade> =
            ExtensionPointName.create("org.jetbrains.kotlin.ideScriptConfigurationControlFacade")
    }
}
