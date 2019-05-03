/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin.compiler.lower

import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import androidx.compose.plugins.kotlin.analysis.ComponentMetadata
import androidx.compose.plugins.kotlin.compiler.ir.buildWithScope

// TODO: Create lower function for user when companion already exists.
fun generateComponentCompanionObject(
    context: GeneratorContext,
    componentMetadata: ComponentMetadata
): IrClass {
    val companion = context.symbolTable.declareClass(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        (componentMetadata.descriptor).companionObjectDescriptor!!
    )
    companion.declarations.add(
        generateCreateInstanceFunction(
            context,
            componentMetadata,
            companion
        )
    )
    return companion
}

fun generateCreateInstanceFunction(
    context: GeneratorContext,
    componentMetadata: ComponentMetadata,
    companion: IrClass
): IrSimpleFunction {
    return context.symbolTable.declareSimpleFunction(
        -1,
        -1,
        IrDeclarationOrigin.DEFINED,
        companion.descriptor.unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier("createInstance"), NoLookupLocation.FROM_BACKEND).single()
    ).buildWithScope(context) { irFunction ->

//        irFunction.createParameterDeclarations()

        val constructorDescriptor =
            componentMetadata.wrapperViewDescriptor.unsubstitutedPrimaryConstructor
        val wrapperViewInstance = IrCallImpl(
            -1,
            -1,
            constructorDescriptor.returnType.toIrType()!!,
            context.symbolTable.referenceConstructor(constructorDescriptor)
        )
        wrapperViewInstance.putValueArgument(
            0,
            IrGetValueImpl(-1, -1, irFunction.valueParameters[0].symbol)
        )
        irFunction.body = IrBlockBodyImpl(
            -1,
            -1, listOf(
                IrReturnImpl(
                    -1,
                    -1,
                    irFunction.symbol.descriptor.returnType!!.toIrType()!!,
                    irFunction.symbol, wrapperViewInstance
                )
            )
        )
    }
}
