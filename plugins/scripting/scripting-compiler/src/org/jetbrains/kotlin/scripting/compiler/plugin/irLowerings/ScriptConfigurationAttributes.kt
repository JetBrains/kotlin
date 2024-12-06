/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings

import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.script.experimental.api.ScriptCompilationConfiguration

private object ScriptCompilationConfigurationAttribute : FirDeclarationDataKey()

data class ScriptResultFieldData(
    val scriptClassName: FqName,
    val fieldName: Name,
    val fieldTypeName: String,
)

var FirScript.scriptCompilationConfigurationAttr: ScriptCompilationConfiguration? by FirDeclarationDataRegistry.data(ScriptCompilationConfigurationAttribute)
var IrScript.scriptCompilationConfigurationAttr: ScriptCompilationConfiguration? by irAttribute(followAttributeOwner = true)
var IrClass.scriptResultFieldDataAttr: ScriptResultFieldData? by irAttribute(followAttributeOwner = true)
