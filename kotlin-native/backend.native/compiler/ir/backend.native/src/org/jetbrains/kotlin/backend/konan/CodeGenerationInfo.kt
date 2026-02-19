/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.objcinterop.isExternalObjCClass

/**
 * We don't need to generate RTTI in some cases, e.g. Objective-C external classes.
 */
internal fun IrClass.requiresRtti(): Boolean = when {
    // TODO: Support more cases
    // Sadly, we still need to emit RTTI for Kotlin inheritors of Obj-C classes.
    // The reason for it is that we need to know a layout of the object to correctly
    // deinitialize it.
    this.isExternalObjCClass() -> false
    else -> true
}

internal fun IrClass.requiresCodeGeneration(): Boolean =
        // For now these two sets (classes that require RTTI and classes that require codegen)
        // are the same, but they might diverge later.
        requiresRtti()