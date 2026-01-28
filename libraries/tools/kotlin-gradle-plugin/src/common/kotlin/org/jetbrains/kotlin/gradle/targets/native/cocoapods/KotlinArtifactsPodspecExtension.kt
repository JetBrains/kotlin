/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.cocoapods

import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.findExtension

abstract class KotlinArtifactsPodspecExtension {

    internal val attributes: Map<String, String>
        field = mutableMapOf<String, String>()

    internal val rawStatements: List<String>
        field = mutableListOf<String>()

    /**
     * Appends an attribute to the generated podspec
     */
    fun attribute(key: String, value: String) {
        attributes[key] = value
    }

    /**
     * Appends a statement 'as is' to the end of the generated podspec
     */
    fun rawStatement(statement: String) {
        rawStatements.add(statement)
    }
}

internal val ExtensionAware.kotlinArtifactsPodspecExtension: KotlinArtifactsPodspecExtension?
    get() = findExtension(KotlinCocoapodsPlugin.ARTIFACTS_PODSPEC_EXTENSION_NAME)