/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.atomicfu.compiler.backend

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.render

internal fun IrProperty.atomicfuRender(): String =
    (if (isVar) "var" else "val") + " " + name.asString() + ": " + backingField?.type?.render()

/**
 * Gets the value of the [IrParameterKind.ExtensionReceiver] argument or returns null if the corresponding parameter does not exist.
 */
internal fun IrFunctionAccessExpression.getExtensionReceiver(): IrExpression? =
    extensionReceiverParameterIndex.let {
        if (it < 0) return null
        return arguments[it]
    }

internal val IrFunctionAccessExpression.extensionReceiverParameterIndex: Int
    get() = symbol.owner.parameters.indexOfFirst { it.kind == IrParameterKind.ExtensionReceiver }
