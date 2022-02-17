/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.File
import java.io.Serializable

interface IdeaKotlinLanguageSettings : Serializable {
    val languageVersion: String?
    val apiVersion: String?
    val isProgressiveMode: Boolean
    val enabledLanguageFeatures: Set<String>
    val optInAnnotationsInUse: Set<String>
    val compilerPluginArguments: List<String>
    val compilerPluginClasspath: List<File>
    val freeCompilerArgs: List<String>
}

fun IdeaKotlinLanguageSettings.deepCopy(interner: Interner = Interner.default()): IdeaKotlinLanguageSettings {
    return IdeaKotlinLanguageSettingsImpl(
        languageVersion = interner.intern(languageVersion),
        apiVersion = interner.intern(apiVersion),
        isProgressiveMode = isProgressiveMode,
        enabledLanguageFeatures = interner.internSet(enabledLanguageFeatures),
        optInAnnotationsInUse = interner.internSet(optInAnnotationsInUse),
        compilerPluginArguments = interner.internList(compilerPluginArguments),
        compilerPluginClasspath = interner.internList(compilerPluginClasspath),
        freeCompilerArgs = interner.internList(freeCompilerArgs)
    )
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinLanguageSettingsImpl(
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val isProgressiveMode: Boolean,
    override val enabledLanguageFeatures: Set<String>,
    override val optInAnnotationsInUse: Set<String>,
    override val compilerPluginArguments: List<String>,
    override val compilerPluginClasspath: List<File>,
    override val freeCompilerArgs: List<String>
) : IdeaKotlinLanguageSettings {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
