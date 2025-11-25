/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageCase
import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.backend.common.lower.AbstractFunctionReferenceLowering
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.customNameInReflection
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSources.File as PLFile

// [NativeSuspendFunctionsLowering] checks annotation of an extension receiver parameter type.
// Unfortunately, it can't be checked on invoke method of lambda/reference, as it can't
// distinguish between extension receiver and first argument. So we just store it in attribute of invoke function
var IrFunction.isRestrictedSuspensionInvokeMethod by irFlag<IrFunction>(copyByDefault = true)

internal class NativeFunctionReferenceLowering(val generationState: NativeGenerationState) : AbstractFunctionReferenceLowering<Context>(generationState.context) {
    companion object {
        private val DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL = IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")

        fun isLoweredFunctionReference(declaration: IrDeclaration): Boolean =
                declaration.origin == DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    }

    private val irBuiltIns = context.irBuiltIns
    private val symbols = context.symbols

    private val kFunctionImplSymbol = symbols.kFunctionImpl
    private val kFunctionDescriptionCorrectSymbol = symbols.kFunctionDescriptionCorrect
    private val kFunctionDescriptionLinkageErrorSymbol = symbols.kFunctionDescriptionLinkageError
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
            val baseName = if (reference.reflectionTargetLinkageError != null) {
                "FUNCTION_REFERENCE_FOR_MISSING_DECLARATION$"
            } else {
                "FUNCTION_REFERENCE_FOR$${reflectionTarget.name}$"
            }
            generationState.fileLowerState.getFunctionReferenceImplUniqueName(baseName).synthesizedName
        }
    }

    override fun getSuperClassType(reference: IrRichFunctionReference): IrType {
        return when {
            reference.reflectionTargetSymbol == null -> irBuiltIns.anyType
            reference.invokeFunction.isSuspend -> kSuspendFunctionImplSymbol.typeWith(listOf(reference.invokeFunction.returnType))
            else -> kFunctionImplSymbol.typeWithArguments(listOf(reference.invokeFunction.returnType))
        }
    }

    override fun getClassOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    override fun getConstructorOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    override fun getInvokeMethodOrigin(reference: IrRichFunctionReference): IrDeclarationOrigin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    override fun getConstructorCallOrigin(reference: IrRichFunctionReference): IrStatementOrigin? = null


    override fun IrBuilderWithScope.generateSuperClassConstructorCall(constructor: IrConstructor, superClassType: IrType, functionReference: IrRichFunctionReference): IrDelegatingConstructorCall {
        return irDelegatingConstructorCall(superClassType.classOrFail.owner.primaryConstructor!!).apply {
            functionReference.reflectionTargetSymbol?.let {
                val reflectionTargetLinkageError = functionReference.reflectionTargetLinkageError
                val description = KFunctionDescription(functionReference)
                arguments[0] = irKFunctionDescription(functionReference, description, reflectionTargetLinkageError)
                typeArguments[0] = functionReference.invokeFunction.returnType
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

    private fun IrBuilderWithScope.irKFunctionDescription(functionReference: IrRichFunctionReference, description: KFunctionDescription, reflectionTargetLinkageError: PartialLinkageCase?): IrConstantValue {
        if (reflectionTargetLinkageError != null) {
            val errorMessage = generationState.context.partialLinkageSupport.prepareLinkageError(
                    doNotLog = true,
                    reflectionTargetLinkageError,
                    functionReference,
                    PLFile.determineFileFor(functionReference.invokeFunction),
            )
            return irConstantObject(
                    kFunctionDescriptionLinkageErrorSymbol.owner,
                    mapOf(
                            "name" to irConstantPrimitive(irString(description.getName())),
                            "reflectionTargetLinkageError" to irConstantPrimitive(irString(errorMessage))
                    )
            )
        } else {
            val kTypeGenerator = toNativeConstantReflectionBuilder(symbols)
            return irConstantObject(
                    kFunctionDescriptionCorrectSymbol.owner,
                    mapOf(
                            "flags" to irConstantPrimitive(irInt(description.getFlags())),
                            "arity" to irConstantPrimitive(irInt(description.getArity())),
                            "fqName" to irConstantPrimitive(irString(description.getFqName())),
                            "name" to irConstantPrimitive(irString(description.getName())),
                            "returnType" to kTypeGenerator.irKType(description.returnType()),
                    )
            )
        }
    }

    private class KFunctionDescription(
            private val functionReference: IrRichFunctionReference,
    ) {
        private val functionReferenceReflectionTarget = functionReference.reflectionTargetSymbol!!.owner

        // this value is used only for hashCode and equals, to distinguish different wrappers on same functions
        fun getFlags(): Int {
            return listOfNotNull(
                    (1 shl 0).takeIf { functionReference.invokeFunction.isSuspend },
                    (1 shl 1).takeIf { functionReference.hasVarargConversion },
                    (1 shl 2).takeIf { functionReference.hasSuspendConversion },
                    (1 shl 3).takeIf { functionReference.hasUnitConversion },
                    (1 shl 4).takeIf { isFunInterfaceConstructorAdapter() },
            ).sum()
        }

        fun getFqName(): String {
            return when {
                isFunInterfaceConstructorAdapter() -> functionReference.invokeFunction.returnType.getClass()!!.fqNameForIrSerialization.toString()
                else -> functionReferenceReflectionTarget.computeFullName()
            }
        }

        fun getName(): String {
            return (functionReferenceReflectionTarget as? IrSimpleFunction?)?.customNameInReflection?.asString()
                    ?: functionReferenceReflectionTarget.name.asString()
        }

        fun getArity(): Int {
            return functionReference.invokeFunction.parameters.size - functionReference.boundValues.size + if (functionReference.invokeFunction.isSuspend) 1 else 0
        }

        fun returnType(): IrType {
            return functionReferenceReflectionTarget.returnType
        }

        private fun isFunInterfaceConstructorAdapter() =
                functionReference.invokeFunction.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR
    }
}
