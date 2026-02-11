/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

import org.jetbrains.kotlin.name.ClassId

public data class TypeScriptDefinitionsFragment(
    public val raw: String,
    public val importedTypes: Map<ClassId, String>,
    public val exportedTypes: Map<ClassId, String>,
)