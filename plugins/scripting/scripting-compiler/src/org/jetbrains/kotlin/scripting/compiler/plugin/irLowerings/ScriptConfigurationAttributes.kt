/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class ScriptResultFieldData(
    val scriptClassName: FqName,
    val fieldName: Name,
    val fieldTypeName: String,
)

var IrClass.scriptResultFieldDataAttr: ScriptResultFieldData? by irAttribute(copyByDefault = true)
