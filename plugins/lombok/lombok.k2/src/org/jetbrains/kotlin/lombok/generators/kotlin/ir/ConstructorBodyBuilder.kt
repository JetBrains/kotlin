/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators.kotlin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.lombok.generators.ConstructorGeneratorKey

/**
 * Both bodies `AbstractConstructorGeneratorPart` leaves for the backend: the generated no-args constructor's,
 * and that of the static factory a `staticName` asks for. Neither is written in FIR, so the two halves of one
 * feature stay in one place instead of being split across the two representations.
 *
 * The constructor's body is a delegating call to the superclass's no-args constructor and nothing else. Built
 * after fir2ir so that the class's property initializers and `init` blocks are not inlined into it - an
 * initializer referencing a primary constructor parameter crashed the JVM backend with "No mapping for symbol"
 * (KT-88659). Mirrors the noarg plugin's `generateNoArgConstructorBody` with `invokeInitializers` always off: no
 * `IrInstanceInitializerCall`, so every field keeps its JVM default until someone assigns it.
 *
 * Every constructor generated for a Kotlin class arrives here bodiless, an inner and a local class being
 * excluded from generation altogether.
 */
object ConstructorBodyBuilder : IrBodyBuilder<ConstructorGeneratorKey>() {
    /**
     * The static factory: `return Entity()` and nothing else, the function existing only so that the no-args
     * constructor beside it can be called by that name.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun IrBlockBodyBuilder.build(key: ConstructorGeneratorKey, declaration: IrSimpleFunction) {
        // The return type is the constructed type already, carrying the factory's own type parameters as its
        // arguments - the very ones the constructor call has to be given.
        val constructedClass = declaration.returnType.classOrNull?.owner
            ?: error("No constructed class for ${declaration.render()}")

        val constructor = constructedClass.constructors.firstOrNull(IrConstructor::takesNoArguments)
            ?: error("No no-args constructor for ${constructedClass.render()}")

        // `irCallConstructor` types the call from the constructor's own return type, which carries the class's
        // type parameters rather than the factory's copies of them, and the IR validator rejects a reference to
        // a type parameter from another scope. The factory's return type is that same type already substituted.
        val constructorCall = irCallConstructor(constructor.symbol, declaration.typeParameters.map { it.defaultType })
        constructorCall.type = declaration.returnType

        +irReturn(constructorCall)
    }

    /**
     * The superclass constructor is resolved here rather than carried over from FIR, the way the noarg plugin
     * does it; `AbstractConstructorGeneratorPart` only generates when it exists, hence the error for the
     * impossible case.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun buildConstructorBody(context: IrPluginContext, constructor: IrConstructor) {
        val ownerClass = constructor.parent as? IrClass ?: return

        val superClass = ownerClass.superTypes.mapNotNull(IrType::getClass).singleOrNull { it.kind == ClassKind.CLASS }
            ?: context.irBuiltIns.anyClass.owner

        val superConstructor = superClass.constructors.firstOrNull(IrConstructor::takesNoArguments)
            ?: error("No no-args superclass constructor for ${ownerClass.render()}")

        constructor.body = context.irFactory.createBlockBody(
            constructor.startOffset, constructor.endOffset,
            listOf(
                IrDelegatingConstructorCallImpl(
                    constructor.startOffset, constructor.endOffset, context.irBuiltIns.unitType,
                    superConstructor.symbol, typeArgumentsCount = 0,
                )
            )
        )
    }
}

/**
 * Whether [this] can be called with no arguments at all. A dispatch receiver doesn't count - an inner class's
 * constructor takes the outer instance as one - and neither does a type parameter.
 */
private val IrConstructor.takesNoArguments: Boolean
    get() = parameters.none { it.kind == IrParameterKind.Regular || it.kind == IrParameterKind.Context }
