/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.explicitApiModeAsCompilerArg
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal val SyncLanguageSettingsWithKotlinExtensionSetupAction = KotlinProjectSetupCoroutine {
    for (sourceSet in kotlinExtension.awaitSourceSets()) {
        val languageSettings = sourceSet.languageSettings
        if (languageSettings !is DefaultLanguageSettingsBuilder) continue

        val isMainSourceSet = sourceSet
            .internal
            .awaitPlatformCompilations()
            .any { KotlinSourceSetTree.orNull(it) == KotlinSourceSetTree.main }

        if (isMainSourceSet) {
            languageSettings.explicitApi = project.providers.provider {
                project.kotlinExtension.explicitApiModeAsCompilerArg()
            }
        }

        languageSettings.freeCompilerArgsProvider = project.provider {
            val propertyValue = with(project.extensions.extraProperties) {
                val sourceSetFreeCompilerArgsPropertyName = "kotlin.mpp.freeCompilerArgsForSourceSet.${sourceSet.name}"
                if (has(sourceSetFreeCompilerArgsPropertyName)) {
                    get(sourceSetFreeCompilerArgsPropertyName)
                } else null
            }

            mutableListOf<String>().apply {
                when (propertyValue) {
                    is String -> add(propertyValue)
                    is Iterable<*> -> addAll(propertyValue.map { it.toString() })
                }
            }
        }
    }
}
