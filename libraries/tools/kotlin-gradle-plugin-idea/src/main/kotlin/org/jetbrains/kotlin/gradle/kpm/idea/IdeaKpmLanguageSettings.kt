/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.File
import java.io.Serializable

sealed interface IdeaKpmLanguageSettings : Serializable {
    val languageVersion: String?
    val apiVersion: String?
    val isProgressiveMode: Boolean
    val enabledLanguageFeatures: Set<String>
    val optInAnnotationsInUse: Set<String>
    val compilerPluginArguments: List<String>
    val compilerPluginClasspath: List<File>
    val freeCompilerArgs: List<String>
}

@InternalKotlinGradlePluginApi
data class IdeaKpmLanguageSettingsImpl(
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val isProgressiveMode: Boolean,
    override val enabledLanguageFeatures: Set<String>,
    override val optInAnnotationsInUse: Set<String>,
    override val compilerPluginArguments: List<String>,
    override val compilerPluginClasspath: List<File>,
    override val freeCompilerArgs: List<String>
) : IdeaKpmLanguageSettings {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
