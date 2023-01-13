/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.cocoapods

import org.gradle.api.*
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.findExtension
import javax.inject.Inject

abstract class KotlinArtifactsPodspecExtension @Inject constructor(private val project: Project) {

    private val attrs = mutableMapOf<String, String>()
    private val statements = mutableListOf<String>()

    internal val attributes: Map<String, String>
        get() = attrs

    internal val rawStatements: List<String>
        get() = statements

    fun attribute(key: String, value: String) {
        attrs[key] = value
    }

    fun rawStatement(statement: String) {
        statements.add(statement)
    }
}

val ExtensionAware.kotlinArtifactsPodspecExtension: KotlinArtifactsPodspecExtension?
    get() = findExtension(KotlinCocoapodsPlugin.ARTIFACTS_PODSPEC_EXTENSION_NAME)