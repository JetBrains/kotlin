/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.impl.toClassPathOrEmpty
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

fun Project.getKtFile(
    virtualFile: VirtualFile?,
    ktFile: KtFile? = null
): KtFile? {
    if (virtualFile == null) return null
    if (ktFile != null) {
        check(ktFile.originalFile.virtualFile == virtualFile)
        return ktFile
    } else {
        return runReadAction { PsiManager.getInstance(this).findFile(virtualFile) as? KtFile }
    }
}

/**
 * For using in DefaultScriptConfigurationManager and in tests only
 */
fun areSimilar(old: ScriptCompilationConfigurationWrapper, new: ScriptCompilationConfigurationWrapper): Boolean {
    if (old.script != new.script) return false

    val oldConfig = old.configuration
    val newConfig = new.configuration

    if (oldConfig == newConfig) return true
    if (oldConfig == null || newConfig == null) return false

    if (oldConfig[ScriptCompilationConfiguration.jvm.jdkHome] != newConfig[ScriptCompilationConfiguration.jvm.jdkHome]) return false

    // there is differences how script definition classpath is added to script classpath in old and new scripting API,
    // so it's important to compare the resulting classpath list, not only the value of key
    if (oldConfig[ScriptCompilationConfiguration.dependencies].toClassPathOrEmpty() != newConfig[ScriptCompilationConfiguration.dependencies].toClassPathOrEmpty()) return false

    if (oldConfig[ScriptCompilationConfiguration.ide.dependenciesSources] != newConfig[ScriptCompilationConfiguration.ide.dependenciesSources]) return false
    if (oldConfig[ScriptCompilationConfiguration.defaultImports] != newConfig[ScriptCompilationConfiguration.defaultImports]) return false

    return true
}