/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet
import org.jetbrains.kotlin.gradle.util.buildProject
import org.junit.Test
import kotlin.test.assertTrue

class KT55347JsProjectImport {

    @Test
    fun `GranularMetadataTransformation should be accessible in pure js projects -- IR`() {
        val project = buildProject { plugins.apply("org.jetbrains.kotlin.js") }
        val kotlin = project.kotlinExtension as KotlinJsProjectExtension
        with(kotlin) {
            js(IR) {
                nodejs()
            }
        }
        project.evaluate()
        kotlin.sourceSets
            .filterIsInstance<DefaultKotlinSourceSet>()
            .forEach { assertTrue(it.getDependenciesTransformation().toList().isEmpty()) }
    }
}