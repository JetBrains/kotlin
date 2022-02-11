/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.File

interface IdeaKotlinLanguageSettings {
    val languageVersion: String?
    val apiVersion: String?
    val isProgressiveMode: Boolean
    val enabledLanguageFeatures: Collection<String>
    val optInAnnotationsInUse: Collection<String>
    val compilerPluginArguments: Collection<String>
    val compilerPluginClasspath: Collection<File>
    val freeCompilerArgs: Collection<String>
}

class IdeaKotlinLanguageSettingsImpl @KotlinGradlePluginApi constructor(
    override val languageVersion: String?,
    override val apiVersion: String?,
    override val isProgressiveMode: Boolean,
    override val enabledLanguageFeatures: Collection<String>,
    override val optInAnnotationsInUse: Collection<String>,
    override val compilerPluginArguments: Collection<String>,
    override val compilerPluginClasspath: Collection<File>,
    override val freeCompilerArgs: Collection<String>
) : IdeaKotlinLanguageSettings
