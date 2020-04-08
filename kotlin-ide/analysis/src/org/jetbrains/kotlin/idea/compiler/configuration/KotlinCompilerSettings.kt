/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.compiler.configuration

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.SettingConstants
import org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMPILER_SETTINGS_SECTION

@State(name = KOTLIN_COMPILER_SETTINGS_SECTION, storages = [(Storage(SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE))])
class KotlinCompilerSettings(project: Project) : BaseKotlinCompilerSettings<CompilerSettings>(project) {
    override fun createSettings() = CompilerSettings()


    companion object {
        fun getInstance(project: Project) = ServiceManager.getService(project, KotlinCompilerSettings::class.java)!!
    }
}
