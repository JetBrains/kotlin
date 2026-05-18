/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.isUnit
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Replace `IrGetObjectValue` with an empty `IrComposite` (type `Unit`) when the referenced
 * object's primary constructor is trivial. Must run before `ObjectClassLowering`, which
 * would otherwise rewrite the access into an `IrGetField` against a static instance global.
 *
 * Motivation is the CUDA device path — class declarations are filtered out of the device
 * LLVM module, so `Unit`, companion-object markers, and similar pure-marker singletons end
 * up as unresolved field references in the kernel. With this shortcut, the singleton access
 * becomes a no-op whose result is dead code. For non-device builds the lowering acts as a
 * minor size win: trivial singleton instances no longer need to be loaded from their static
 * globals at use sites where the value is unused.
 *
 * "Trivial" here means: no anonymous initializer blocks, no field/property initializers,
 * and the primary constructor body contains only a delegating super-constructor call and/or
 * the implicit `IrInstanceInitializerCall` placeholder. Objects with observable construction
 * side effects are left alone.
 */
internal class DropTrivialObjectInstancesLowering(val context: Context) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        val unitType = context.irBuiltIns.unitType
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val clazz = expression.symbol.owner
                if (!clazz.isObject) return expression
                // `Unit` is handled specially by codegen (its instance pointer is the
                // dedicated `theUnitInstance` global referenced everywhere a `Unit` value
                // appears); leaving it to `ObjectClassLowering` keeps that invariant intact.
                if (clazz.isUnit()) return expression
                if (!clazz.hasTrivialPrimaryConstructor()) return expression
                return IrCompositeImpl(expression.startOffset, expression.endOffset, unitType)
            }
        })
    }
}

private fun IrClass.hasTrivialPrimaryConstructor(): Boolean {
    // Only `Any` as a supertype — any other class or interface in the super list could
    // introduce state, an init contract, or vtable entries we can't elide blindly.
    if (superTypes.singleOrNull()?.isAny() != true) return false
    val ctor = declarations.singleOrNull { it is IrConstructor && it.isPrimary } as? IrConstructor ?: return false
    val loweredCtor = ctor.loweredConstructorFunction
    if (loweredCtor != null) {
        val body = loweredCtor.body as? IrBlockBody ?: return false
        return body.statements.all { it is IrComposite || it is IrReturn }
    } else {
        if (declarations.any { it is IrAnonymousInitializer }) return false
        val hasFieldInitializers = declarations.asSequence()
                .mapNotNull { (it as? IrProperty)?.backingField ?: it as? IrField }
                .any { it.initializer != null }
        if (hasFieldInitializers) return false
        val body = ctor.body as? IrBlockBody ?: return false
        return body.statements.all { it is IrDelegatingConstructorCall || it is IrInstanceInitializerCall }
    }
}
