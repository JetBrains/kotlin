/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.ir.declarations.*

internal val IrFunction.longName: String
        get() = "${(parent as? IrClass)?.name?.asString() ?: "<root>"}.${(this as? IrSimpleFunction)?.name ?: "<init>"}"