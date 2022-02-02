/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.lower.hasAnnotationSafe
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.IrDeserializer.TopLevelSymbolKind.FUNCTION_SYMBOL
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTreeWithSymbols
import org.jetbrains.kotlin.ir.util.DeepCopyTypeRemapper
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.TypeRemapper
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.module

internal interface DecoyTransformBase {
    val context: IrPluginContext
    val signatureBuilder: IdSignatureSerializer

    fun IrFunction.getSignatureId(): Long {
        val signature = symbol.signature
            ?: signatureBuilder.composeSignatureForDeclaration(this, false)

        return signature.getSignatureId()
    }

    private fun IdSignature.getSignatureId(): Long {
        return when (this) {
            is IdSignature.AccessorSignature -> accessorSignature.id!!
            is IdSignature.FileLocalSignature -> id
            is IdSignature.ScopeLocalDeclaration -> id.toLong()
            is IdSignature.SpecialFakeOverrideSignature -> memberSignature.getSignatureId()
            is IdSignature.LoweredDeclarationSignature -> TODO()
            is IdSignature.FileSignature -> TODO()
            is IdSignature.CommonSignature -> id!!
            is IdSignature.CompositeSignature -> this.nearestPublicSig().getSignatureId()
            is IdSignature.LocalSignature -> this.getSignatureId()
        }
    }

    fun irVarargString(valueArguments: List<String>): IrExpression {
        val stringArrayType = IrSimpleTypeImpl(
            classifier = context.irBuiltIns.arrayClass,
            hasQuestionMark = false,
            arguments = listOf(context.irBuiltIns.stringType as IrTypeArgument),
            annotations = emptyList()
        )
        return IrVarargImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            type = stringArrayType,
            varargElementType = context.irBuiltIns.stringType,
            elements = valueArguments.map { it.toIrConst(context.irBuiltIns.stringType) }
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun IrFunction.getComposableForDecoy(): IrFunctionSymbol {
        val implementationName = getDecoyTargetName()
        val signatureId = getSignatureId()
        val implementation = (parent as? IrDeclarationContainer)?.declarations
            ?.filterIsInstance<IrFunction>()
            ?.firstOrNull {
                it.getDecoyImplementationName() == implementationName &&
                    it.getDecoyImplementationId() == signatureId
            }

        if (implementation != null) {
            return implementation.symbol
        }

        val signature = getDecoySignature()
        require(signature.size == 4) {
            "Could not find local implementation for $implementationName"
        }
        // top-level
        val idSig = IdSignature.CommonSignature(
            packageFqName = signature[0],
            declarationFqName = signature[1],
            id = signature[2].toLongOrNull(),
            mask = signature[3].toLong()
        )

        val linker = (context as IrPluginContextImpl).linker

        val symbol = if (isTopLevel) {
            linker.getDeclaration(module, idSig) as? IrSimpleFunctionSymbol
        } else {
            (parent as? IrDeclarationContainer)?.declarations
                ?.filterIsInstance<IrFunction>()
                ?.find {
                    it.symbol.signature == idSig
                }
                ?.symbol
        }

        return symbol ?: error("Couldn't find implementation for $name")
    }

    fun IrFunction.getDecoyImplementationName(): String? {
        val annotation = getAnnotation(DecoyFqNames.DecoyImplementation) ?: return null

        @Suppress("UNCHECKED_CAST")
        val decoyImplName = annotation.getValueArgument(0) as IrConst<String>

        return decoyImplName.value
    }

    fun IrFunction.getDecoyImplementationId(): Long? {
        val annotation = getAnnotation(DecoyFqNames.DecoyImplementation) ?: return null

        @Suppress("UNCHECKED_CAST")
        val decoyImplName = annotation.getValueArgument(1) as IrConst<Long>

        return decoyImplName.value
    }

    private fun IrFunction.getDecoyTargetName(): String {
        val annotation = getAnnotation(DecoyFqNames.Decoy)!!
        @Suppress("UNCHECKED_CAST")
        val decoyTargetName = annotation.getValueArgument(0) as IrConst<String>

        return decoyTargetName.value
    }

    private fun IrFunction.getDecoySignature(): List<String> {
        val annotation = getAnnotation(DecoyFqNames.Decoy)!!
        val decoyVararg = annotation.getValueArgument(1) as IrVararg

        @Suppress("UNCHECKED_CAST")
        return decoyVararg.elements.map {
            (it as IrConst<String>).value
        }
    }

    private fun IrDeserializer.getDeclaration(
        moduleDescriptor: ModuleDescriptor,
        idSignature: IdSignature
    ): IrSymbol = resolveBySignatureInModule(idSignature, FUNCTION_SYMBOL, moduleDescriptor.name)
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrDeclaration.isDecoy(): Boolean =
    hasAnnotationSafe(DecoyFqNames.Decoy)

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrDeclaration.isDecoyImplementation(): Boolean =
    hasAnnotationSafe(DecoyFqNames.DecoyImplementation)

private fun IrFunction.getDecoyImplementationDefaultValuesBitMask(): Int? {
    val annotation = getAnnotation(DecoyFqNames.DecoyImplementationDefaultsBitMask) ?: return null

    @Suppress("UNCHECKED_CAST")
    val paramsDefaultsBitMask = annotation.getValueArgument(0) as IrConst<Int>

    return paramsDefaultsBitMask.value
}

fun IrFunction.didDecoyHaveDefaultForValueParameter(paramIndex: Int): Boolean {
    return getDecoyImplementationDefaultValuesBitMask()?.let {
        it.shr(paramIndex).and(1) == 1
    } ?: false
}

inline fun <reified T : IrElement> T.copyWithNewTypeParams(
    source: IrFunction,
    target: IrFunction
): T {
    return deepCopyWithSymbols(target) { symbolRemapper, typeRemapper ->
        val typeParamRemapper = object : TypeRemapper by typeRemapper {
            override fun remapType(type: IrType): IrType {
                return typeRemapper.remapType(type.remapTypeParameters(source, target))
            }
        }

        val deepCopy = DeepCopyIrTreeWithSymbols(symbolRemapper, typeParamRemapper)
        (typeRemapper as? DeepCopyTypeRemapper)?.deepCopy = deepCopy
        deepCopy
    }
}