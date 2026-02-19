/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*

internal class PropertyReferencesConstructorsSet(
        val local: IrConstructorSymbol,
        val byRecieversCount: List<IrConstructorSymbol>
) {
    constructor(local: IrClassSymbol, byRecieversCount: List<IrClassSymbol>) : this(
            local.constructors.single(),
            byRecieversCount.map { it.constructors.single() }
    )
}

internal val BackendNativeSymbols.immutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
            kLocalDelegatedPropertyImpl,
            listOf(kProperty0Impl, kProperty1Impl, kProperty2Impl)
    )

internal val BackendNativeSymbols.mutablePropertiesConstructors
    get() = PropertyReferencesConstructorsSet(
            kLocalDelegatedMutablePropertyImpl,
            listOf(kMutableProperty0Impl, kMutableProperty1Impl, kMutableProperty2Impl)
    )

internal class PropertyReferenceLowering(generationState: NativeGenerationState) : AbstractPropertyReferenceLowering<Context>(generationState.context) {
    private val symbols = context.symbols
    private val immutableSymbols = symbols.immutablePropertiesConstructors
    private val mutableSymbols = symbols.mutablePropertiesConstructors

    override fun functionReferenceClass(arity: Int): IrClassSymbol {
        return context.irBuiltIns.kFunctionN(arity).symbol
    }

    override fun IrBuilderWithScope.createKProperty(
            reference: IrRichPropertyReference,
            typeArguments: List<IrType>,
            getterReference: IrRichFunctionReference,
            setterReference: IrRichFunctionReference?,
    ): IrExpression {
        val constructor = if (setterReference != null) {
            mutableSymbols
        } else {
            immutableSymbols
        }.byRecieversCount[typeArguments.size - 1]
        return irCall(constructor, reference.type, typeArguments).apply {
            arguments[0] = propertyReferenceNameExpression(reference)
            arguments[1] = propertyReferenceLinkageErrorExpression(reference)
            arguments[2] = getterReference
            setterReference?.let { arguments[3] = it }
        }
    }

    override fun IrBuilderWithScope.createLocalKProperty(
            reference: IrRichPropertyReference,
            propertyName: String,
            propertyType: IrType,
            isMutable: Boolean,
    ): IrConstantValue {
        val constructor = (if (isMutable) mutableSymbols else immutableSymbols).local.owner
        return toNativeConstantReflectionBuilder(symbols).run {
            irConstantObject(
                    constructor.constructedClass,
                    mapOf(
                            "name" to irConstantPrimitive(irString(propertyName)),
                            "returnType" to irKType(propertyType)
                    )
            )
        }
    }
}

