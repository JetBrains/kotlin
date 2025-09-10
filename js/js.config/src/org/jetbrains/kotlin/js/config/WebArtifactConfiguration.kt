/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.config

import java.io.File

/**
 * @property moduleName The name of the compilation module.
 * @property outputDirectory The destination for the generated files
 * @property outputName The base name for generated files.
 * @property granularity The ranularity of JS files generation.
 */
data class WebArtifactConfiguration(
    val moduleKind: ModuleKind,
    val moduleName: String,
    val outputDirectory: File,
    val outputName: String,
    val granularity: JsGenerationGranularity,
    val tsCompilationStrategy: TsCompilationStrategy,
)
