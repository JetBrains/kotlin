/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.generators.kotlin.ir

import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.lombok.generators.BuilderDeclarationType
import org.jetbrains.kotlin.lombok.generators.BuilderGeneratorKey
import org.jetbrains.kotlin.name.Name

/**
 * Fills the bodies with the builder members previously generated (as bare signatures) by
 * [org.jetbrains.kotlin.lombok.generators.BuilderGenerator] for a Kotlin class annotated with `@Builder`.
 *
 * For `@Builder class Entity(val a: A, val b: B)` the following bodies are produced:
 *  - setters `a(a)` / `b(b)`: store the argument into the builder's backing field and return `this`;
 *  - `build()`: invoke the entity constructor with the accumulated fields;
 *  - `builder()`: return a fresh builder instance;
 *  - `toBuilder()`: return a builder pre-filled from the receiver's properties.
 *
 */
object BuilderBodyBuilder : IrBodyBuilder<BuilderGeneratorKey>() {
    override fun IrBlockBodyBuilder.build(
        key: BuilderGeneratorKey,
        declaration: IrSimpleFunction,
    ) {
        val regularParameters = declaration.parameters.filter { it.kind == IrParameterKind.Regular }
        when (key.type) {
            BuilderDeclarationType.Setter -> buildSetter(declaration, regularParameters.single())
            BuilderDeclarationType.Build -> buildBuildMethod(declaration)
            BuilderDeclarationType.Builder -> buildBuilderFactory(declaration)
            BuilderDeclarationType.ToBuilder -> buildToBuilder(declaration)
            else -> {}
        }
    }

    private fun IrBlockBodyBuilder.buildSetter(declaration: IrSimpleFunction, parameter: IrValueParameter) {
        val builderClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val field = builderClass.findBuilderField(parameter.name) ?: return

        +irSetField(irGet(thisParameter), field, irGet(parameter))
        +irReturn(irGet(thisParameter))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildBuildMethod(declaration: IrSimpleFunction) {
        val builderClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val entityClass = declaration.returnType.classOrNull!!.owner
        val constructor = entityClass.builderConstructor()

        val constructorCall = irConstruct(declaration, constructor)
        constructor.parameters.forEachIndexed { index, parameter ->
            if (parameter.kind != IrParameterKind.Regular) return@forEachIndexed
            val field = builderClass.findBuilderField(parameter.name) ?: return@forEachIndexed
            constructorCall.arguments[index] = irGetField(irGet(thisParameter), field)
        }

        +irReturn(constructorCall)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildBuilderFactory(declaration: IrSimpleFunction) {
        val builderClass = declaration.returnType.classOrNull!!.owner
        +irReturn(irConstruct(declaration, builderClass.builderConstructor()))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrBlockBodyBuilder.buildToBuilder(declaration: IrSimpleFunction) {
        val entityClass = declaration.parent as IrClass
        val thisParameter = declaration.dispatchReceiverParameter!!
        val builderClass = declaration.returnType.classOrNull!!.owner

        val builder = irTemporary(irConstruct(declaration, builderClass.builderConstructor()), nameHint = "builder")
        val fields = builderClass.declarations.mapNotNull { (it as? IrProperty)?.backingField }
        for (field in fields) {
            val property = entityClass.findDeclaration<IrProperty> { it.name == field.name } ?: continue
            val getter = property.getter ?: continue
            +irSetField(
                irGet(builder),
                field,
                irCall(getter.symbol).apply { arguments[0] = irGet(thisParameter) },
            )
        }

        +irReturn(irGet(builder))
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.findBuilderField(name: Name): IrField? =
        declarations.firstNotNullOfOrNull { declaration ->
            val field = (declaration as? IrProperty)?.backingField
            field?.takeIf { it.name == name }
        }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.builderConstructor(): IrConstructor =
        primaryConstructor ?: constructors.first()

    /**
     * Builds `constructor(...)` for the value this function returns, i.e. the entity in `build` or the
     * builder in `builder`/`toBuilder`.
     *
     * Both the call's type arguments and its result type are taken from [declaration]'s return type rather
     * than from the constructor's own declaration. This matters for generic classes (KT-83334):
     *  - the type arguments must be present, otherwise a constructor call with zero type arguments for a
     *    generic class crashes JVM synthetic-accessor lowering;
     *  - the result type must reference the type parameter that is in scope at the call site (this
     *    function's / its class's parameter), not the constructed class's own parameter. For a factory
     *    like `fun <T> builder(): FooBuilder<T>` the constructor's declared return type `FooBuilder<T of
     *    FooBuilder>` mentions an out-of-scope parameter and fails IR validation.
     */
    private fun IrBlockBodyBuilder.irConstruct(declaration: IrSimpleFunction, constructor: IrConstructor): IrConstructorCall =
        irCallConstructor(constructor.symbol, declaration.constructedTypeArguments()).apply {
            type = declaration.returnType
        }

    private fun IrSimpleFunction.constructedTypeArguments(): List<IrType> =
        (returnType as? IrSimpleType)?.arguments?.map { it.typeOrFail } ?: emptyList()
}
