/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.AbstractFunctionReferenceLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.name.SpecialNames

// [NativeSuspendFunctionsLowering] checks annotation of an extension receiver parameter type.
// Unfortunately, it can't be checked on invoke method of lambda/reference, as it can't
// distinguish between extension receiver and first argument. So we just store it in attribute of invoke function
var IrFunction.isRestrictedSuspensionInvokeMethod by irFlag<IrFunction>(copyByDefault = true)

/**
 * Processing of `@VolatileLambda` annotation
 */
internal class VolatileLambdaLowering(val generationState: NativeGenerationState) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transform(object : IrTransformer<IrDeclarationParent>() {
            private val stack = mutableListOf<IrElement>()

            override fun visitElement(element: IrElement, data: IrDeclarationParent): IrElement {
                stack.push(element)
                val result = super.visitElement(element, data)
                stack.pop()
                return result
            }

            override fun visitExpression(expression: IrExpression, data: IrDeclarationParent): IrExpression {
                stack.push(expression)
                val result = super.visitExpression(expression, data)
                stack.pop()
                return result
            }

            override fun visitDeclaration(declaration: IrDeclarationBase, data: IrDeclarationParent): IrStatement {
                stack.push(declaration)
                val result = super.visitDeclaration(declaration, declaration as? IrDeclarationParent ?: data)
                stack.pop()
                return result
            }

            override fun visitSpreadElement(spread: IrSpreadElement, data: IrDeclarationParent): IrSpreadElement {
                stack.push(spread)
                val result = super.visitSpreadElement(spread, data)
                stack.pop()
                return result
            }

            private val VOLATILE_LAMBDA_FQ_NAME = FqName.fromSegments(listOf("kotlin", "native", "internal", "VolatileLambda"))

            override fun visitRichFunctionReference(expression: IrRichFunctionReference, data: IrDeclarationParent): IrExpression {
                expression.transformChildren(this, data)
                val irBuilder = generationState.context.createIrBuilder((data as IrSymbolOwner).symbol,
                        expression.startOffset, expression.endOffset)
                for (i in stack.size - 1 downTo 0) {
                    val cur = stack[i]
                    if (cur is IrBlock)
                        continue
                    if (cur !is IrCall)
                        break
                    val argument = if (i < stack.size - 1) stack[i + 1] else expression
                    val parameter = cur.symbol.owner.parameters.singleOrNull { cur.arguments[it] === argument }
                    if (parameter?.annotations?.findAnnotation(VOLATILE_LAMBDA_FQ_NAME) != null) {
                        require(expression.boundValues.isEmpty()) {
                            "@VolatileLambda argument's can't capture"
                        }
                        return irBuilder.irComposite(origin = IrStatementOrigin.LAMBDA) {
                            +expression.invokeFunction
                            +irRawFunctionReference(expression.type, expression.invokeFunction.symbol)
                        }
                    }
                    break
                }
                return expression
            }

            override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent): IrExpression {
                shouldNotBeCalled()
            }
        }, data = irFile)
    }

}

internal class NativeFunctionReferenceLowering(val generationState: NativeGenerationState) : AbstractFunctionReferenceLowering<Context>(generationState.context) {
    companion object {
        private val DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL = IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")

        fun isLoweredFunctionReference(declaration: IrDeclaration): Boolean =
                declaration.origin == DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    }

    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.ir.symbols

    private val kFunctionImplSymbol = symbols.kFunctionImpl
    private val kFunctionDescriptionSymbol = symbols.kFunctionDescription
    private val kSuspendFunctionImplSymbol = symbols.kSuspendFunctionImpl

    override fun postprocessClass(functionReferenceClass: IrClass, functionReference: IrRichFunctionReference) {
        functionReferenceClass.hasSyntheticNameToBeHiddenInReflection = true
    }

    override fun postprocessInvoke(invokeFunction: IrSimpleFunction, functionReference: IrRichFunctionReference) {
        if (functionReference.isRestrictedSuspension) {
            invokeFunction.isRestrictedSuspensionInvokeMethod = true
        }
    }

    override fun getReferenceClassName(reference: IrRichFunctionReference): Name {
        val reflectionTarget = reference.reflectionTargetSymbol?.owner
        return if (reflectionTarget == null) {
            SpecialNames.NO_NAME_PROVIDED
        } else {
            generationState.fileLowerState.getFunctionReferenceImplUniqueName("FUNCTION_REFERENCE_FOR$${reflectionTarget.name}$").synthesizedName
        }
    }

    override fun getSuperClassType(reference: IrRichFunctionReference) : IrType {
        return when {
            reference.reflectionTargetSymbol == null -> irBuiltIns.anyType
            reference.invokeFunction.isSuspend -> kSuspendFunctionImplSymbol.typeWith(listOf(reference.invokeFunction.returnType))
            else -> kFunctionImplSymbol.typeWithArguments(listOf(reference.invokeFunction.returnType))
        }
    }

    override fun getClassOrigin(reference: IrRichFunctionReference) : IrDeclarationOrigin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    override fun getConstructorOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    override fun getInvokeMethodOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    override fun getConstructorCallOrigin(reference: IrRichFunctionReference): IrStatementOrigin? = null


    override fun IrBuilderWithScope.generateSuperClassConstructorCall(superClassType: IrType, functionReference: IrRichFunctionReference) : IrDelegatingConstructorCall {
        return irDelegatingConstructorCall(superClassType.classOrFail.owner.primaryConstructor!!).apply {
            functionReference.reflectionTargetSymbol?.let { reflectionTarget ->
                val description = KFunctionDescription(
                        functionReferenceReflectionTarget = reflectionTarget.owner,
                        referencedFunction = functionReference.invokeFunction,
                        boundParameters = functionReference.boundValues.size,
                        isCoercedToUnit = functionReference.hasUnitConversion,
                        isSuspendConversion = functionReference.hasSuspendConversion,
                        isVarargConversion = functionReference.hasVarargConversion
                )
                typeArguments[0] = functionReference.invokeFunction.returnType
                arguments[0] = irKFunctionDescription(description)
            }
        }
    }

    override fun generateExtraMethods(functionReferenceClass: IrClass, reference: IrRichFunctionReference) {
        if (reference.reflectionTargetSymbol == null) return
        fun addOverrideInner(name: String, value: IrBuilderWithScope.(IrFunction) -> IrExpression) {
            val overridden = functionReferenceClass.superTypes.mapNotNull { superType ->
                superType.getClass()
                        ?.declarations
                        ?.filterIsInstance<IrSimpleFunction>()
                        ?.singleOrNull { it.name.asString() == name }
                        ?.symbol
            }
            require(overridden.isNotEmpty())
            val function = functionReferenceClass.addFunction {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                this.name = Name.identifier(name)
                modality = Modality.FINAL
                returnType = overridden[0].owner.returnType
            }
            function.parameters += function.createDispatchReceiverParameterWithClassParent()
            function.overriddenSymbols += overridden
            function.body = context.createIrBuilder(function.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                +irReturn(value(function))
            }
        }

        val fields = functionReferenceClass.fields.toList()
        when (fields.size) {
            0 -> {}
            1 -> addOverrideInner("computeReceiver") { f ->
                irGetField(irGet(f.dispatchReceiverParameter!!), fields[0])
            }
            else -> TODO("Code generation for references with several bound receivers is not supported yet")
        }
    }

    private fun IrBuilderWithScope.irKFunctionDescription(description: KFunctionDescription): IrConstantValue {
        val kTypeGenerator = toNativeConstantReflectionBuilder(symbols)

        return irConstantObject(
                kFunctionDescriptionSymbol.owner,
                mapOf(
                        "flags" to irConstantPrimitive(irInt(description.getFlags())),
                        "arity" to irConstantPrimitive(irInt(description.getArity())),
                        "fqName" to irConstantPrimitive(irString(description.getFqName())),
                        "name" to irConstantPrimitive(irString(description.getName())),
                        "returnType" to kTypeGenerator.irKType(description.returnType())
                )
        )
    }

    private class KFunctionDescription(
            private val functionReferenceReflectionTarget: IrFunction,
            private val referencedFunction: IrFunction,
            private val boundParameters: Int,
            private val isCoercedToUnit: Boolean,
            private val isSuspendConversion: Boolean,
            private val isVarargConversion: Boolean,
    ) {
        // this value is used only for hashCode and equals, to distinguish different wrappers on same functions
        fun getFlags(): Int {
            return listOfNotNull(
                    (1 shl 0).takeIf { referencedFunction.isSuspend },
                    (1 shl 1).takeIf { isVarargConversion },
                    (1 shl 2).takeIf { isSuspendConversion },
                    (1 shl 3).takeIf { isCoercedToUnit },
                    (1 shl 4).takeIf { isFunInterfaceConstructorAdapter() }
            ).sum()
        }

        fun getFqName(): String {
            return if (isFunInterfaceConstructorAdapter())
                referencedFunction.returnType.getClass()!!.fqNameForIrSerialization.toString()
            else
                functionReferenceReflectionTarget.computeFullName()
        }

        fun getName(): String {
            return (((functionReferenceReflectionTarget as? IrSimpleFunction)?.attributeOwnerId as? IrSimpleFunction)?.name
                    ?: functionReferenceReflectionTarget.name).asString()
        }

        fun getArity(): Int {
            return referencedFunction.parameters.size - boundParameters + if (referencedFunction.isSuspend) 1 else 0
        }

        fun returnType(): IrType {
            return functionReferenceReflectionTarget.returnType
        }

        private fun isFunInterfaceConstructorAdapter() =
                referencedFunction.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR
    }
}
