/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.tsexport

import org.jetbrains.kotlin.ir.backend.js.tsexport.ExportedDeclaration

internal data class TypeScriptModuleArtifact(
    val externalModuleName: String,
    val exportModel: List<ExportedDeclaration>,
    val packageFqn: String? = null, // Only for per-file granularity
)
