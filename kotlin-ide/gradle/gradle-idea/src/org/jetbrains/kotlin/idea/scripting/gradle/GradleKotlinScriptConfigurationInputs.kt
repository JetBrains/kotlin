/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.cache.CachedConfigurationInputs
import org.jetbrains.kotlin.psi.KtFile

/**
 * Up to date of gradle script depends on following factors:
 * 1. It is out of date when essential [sections] are changed
 * @see getGradleScriptInputsStamp
 * 2. When some related file is changed (other gradle script, gradle.properties file)
 * @see GradleScriptInputsWatcher.areRelatedFilesUpToDate
 *
 * [inputsTS] is needed to check if some related file was changed since last update
 */
data class GradleKotlinScriptConfigurationInputs(
    val sections: String,
    val inputsTS: Long
) : CachedConfigurationInputs {
    override fun isUpToDate(project: Project, file: VirtualFile, ktFile: KtFile?): Boolean {
        val actualStamp = getGradleScriptInputsStamp(project, file, ktFile) ?: return false

        if (actualStamp.sections != this.sections) return false

        return project.service<GradleScriptInputsWatcher>().areRelatedFilesUpToDate(file, inputsTS)
    }
}