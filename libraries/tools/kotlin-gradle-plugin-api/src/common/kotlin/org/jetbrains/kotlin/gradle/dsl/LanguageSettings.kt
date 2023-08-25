/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.project.model

interface LanguageSettings {
    val languageVersion: String?
    val apiVersion: String?
    val progressiveMode: Boolean
    val enabledLanguageFeatures: Set<String>
    val optInAnnotationsInUse: Set<String>
}