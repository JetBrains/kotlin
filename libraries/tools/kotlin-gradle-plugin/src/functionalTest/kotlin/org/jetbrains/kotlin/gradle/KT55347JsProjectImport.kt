/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.*
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.junit.Test
import kotlin.test.assertTrue

class KT55347JsProjectImport {
    private fun `GranularMetadataTransformation should be accessible in pure js projects`(jsCompiler: KotlinJsCompilerType) {
        val project = buildProject { plugins.apply("org.jetbrains.kotlin.js") }
        val kotlin = project.kotlinExtension as KotlinJsProjectExtension
        with (kotlin) {
            js(jsCompiler) {
                nodejs()
            }
        }

        project.evaluate()

        kotlin.sourceSets
            .filterIsInstance<DefaultKotlinSourceSet>()
            .forEach { assertTrue(it.getDependenciesTransformation().toList().isEmpty()) }
    }

    @Test
    fun `GranularMetadataTransformation should be accessible in pure js projects -- IR`() =
        `GranularMetadataTransformation should be accessible in pure js projects`(IR)

    @Suppress("DEPRECATION")
    @Test
    fun `GranularMetadataTransformation should be accessible in pure js projects -- BOTH`() =
        `GranularMetadataTransformation should be accessible in pure js projects`(BOTH)
}