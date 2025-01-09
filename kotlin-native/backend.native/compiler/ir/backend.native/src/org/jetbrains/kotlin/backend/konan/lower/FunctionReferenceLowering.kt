/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance

// [NativeSuspendFunctionsLowering] checks annotation of an extension receiver parameter type.
// Unfortunately, it can't be checked on invoke method of lambda/reference, as it can't
// distinguish between extension receiver and first argument. So we just store it in attribute of invoke function
var IrFunction.isRestrictedSuspensionInvokeMethod by irFlag<IrFunction>(followAttributeOwner = true)

/**
 * Transforms a function reference into a subclass of `kotlin.native.internal.KFunctionImpl` and `kotlin.FunctionN`,
 * or `kotlin.native.internal.KSuspendFunctionImpl` and `kotlin.KSuspendFunctionN` (for suspend functions/lambdas),
 * or `Any` (for simple lamdbas), or a custom superclass (in case of SAM conversion).
 *
 * For example, `::bar$lambda$0<BarTP>` in the following code:
 * ```kotlin
 * fun <FooTP> foo(v: FooTP, l: (FooTP) -> String): String {
 *     return l(v)
 * }
 *
 * private fun <T> bar$lambda$0(t: T): String { /* ... */ }
 *
 * fun <BarTP> bar(v: BarTP): String {
 *     return foo(v, ::bar$lambda$0<BarTP>)
 * }
 * ```
 *
 * is lowered into:
 * ```kotlin
 * private class bar$lambda$0$FUNCTION_REFERENCE$0<T> : kotlin.native.internal.KFunctionImpl<String>, kotlin.Function1<T, String> {
 *     override fun invoke(p1: T): String {
 *         return bar$lambda$0<T>(p1)
 *     }
 * }
 *
 * fun <BarTP> bar(v: BarTP): String {
 *     return foo(v, bar$lambda$0$FUNCTION_REFERENCE$0<BarTP>())
 * }
 * ```
 */
internal class FunctionReferenceLowering(val generationState: NativeGenerationState) : FileLoweringPass {
    companion object {
        private val DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL = IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")

        fun isLoweredFunctionReference(declaration: IrDeclaration): Boolean =
                declaration.origin == DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    }

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

                val (clazz, newExpression) = FunctionReferenceBuilder(data, expression, generationState, irBuilder).build()
                return irBuilder.irBlock {
                    +clazz
                    +newExpression
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent): IrExpression {
                shouldNotBeCalled()
            }
        }, data = irFile)
    }

    private class FunctionReferenceBuilder(
            val parent: IrDeclarationParent,
            val functionReference: IrRichFunctionReference,
            val generationState: NativeGenerationState,
            val irBuilder: IrBuilderWithScope,
    ) {
        data class BuiltFunctionReference(val functionReferenceClass: IrClass, val functionReferenceExpression: IrExpression)

        private val context = generationState.context
        private val irBuiltIns = context.irBuiltIns
        private val symbols = context.ir.symbols
        private val irFactory = context.irFactory

        private val startOffset = functionReference.startOffset
        private val endOffset = functionReference.endOffset

        private val functionReferenceClass = irFactory.buildClass {
            startOffset = this@FunctionReferenceBuilder.startOffset
            endOffset = this@FunctionReferenceBuilder.endOffset
            origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
            val reflectionTarget = functionReference.reflectionTargetSymbol?.owner
            if (reflectionTarget == null) {
                name = SpecialNames.NO_NAME_PROVIDED
            } else {
                name = generationState.fileLowerState.getFunctionReferenceImplUniqueName("FUNCTION_REFERENCE_FOR$${reflectionTarget.name}$").synthesizedName
            }
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            hasSyntheticNameToBeHiddenInReflection = true
            parent = this@FunctionReferenceBuilder.parent
            createThisReceiverParameter()
        }

        private val kFunctionImplSymbol = symbols.kFunctionImpl
        private val kFunctionDescriptionSymbol = symbols.kFunctionDescription
        private val kSuspendFunctionImplSymbol = symbols.kSuspendFunctionImpl

        // Sam class used as superclass can sometimes have type projections.
        // But that's not suitable for super-types, so we erase them
        private fun IrType.removeProjections(): IrType {
            if (this !is IrSimpleType) return this
            val arguments = arguments.mapIndexed { index, argument ->
                if (argument is IrTypeProjection && argument.variance == Variance.INVARIANT)
                    argument.type
                else
                    (classifier as IrClassSymbol).owner.typeParameters[index].erasedUpperBound.defaultType
            }
            return classifier.typeWith(arguments)
        }

        private fun buildClass(): IrClass {
            val superClass = when {
                functionReference.reflectionTargetSymbol == null -> irBuiltIns.anyType
                functionReference.invokeFunction.isSuspend -> kSuspendFunctionImplSymbol.typeWith(listOf(functionReference.invokeFunction.returnType))
                else -> kFunctionImplSymbol.typeWithArguments(listOf(functionReference.invokeFunction.returnType))
            }
            val superInterfaceType = functionReference.type.removeProjections()
            functionReferenceClass.superTypes = mutableListOf(superClass, superInterfaceType)
            val constructor = functionReferenceClass.addConstructor {
                this.startOffset = this@FunctionReferenceBuilder.startOffset
                this.endOffset = this@FunctionReferenceBuilder.endOffset
                origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
                isPrimary = true
            }.apply {
                body = context.createIrBuilder(symbol, this.startOffset, this.endOffset).irBlockBody {
                    +irDelegatingConstructorCall(superClass.classOrFail.owner.primaryConstructor!!).apply {
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
                    +IrInstanceInitializerCallImpl(this.startOffset, this.endOffset, functionReferenceClass.symbol, irBuiltIns.unitType)
                }
                parameters = functionReference.boundValues.mapIndexed { index, value ->
                    buildValueParameter(this) {
                        name = Name.identifier("p${index}")
                        startOffset = value.startOffset
                        endOffset = value.endOffset
                        type = value.type
                        kind = IrParameterKind.Regular
                    }
                }
            }

            val fields = functionReference.boundValues.mapIndexed { index, captured ->
                functionReferenceClass.addField {
                    startOffset = captured.startOffset
                    endOffset = captured.endOffset
                    name = Name.identifier("f${'$'}${index}")
                    visibility = DescriptorVisibilities.PRIVATE
                    isFinal = true
                    type = captured.type
                }.apply {
                    val builder = context.createIrBuilder(symbol, startOffset, endOffset)
                    initializer = builder.irExprBody(builder.irGet(constructor.parameters[index]))
                }
            }
            buildInvokeMethod(
                    functionReference.overriddenFunctionSymbol.owner,
                    superInterfaceType,
                    functionReference.invokeFunction,
                    fields
            ).apply {
                if (functionReference.isRestrictedSuspension) {
                    isRestrictedSuspensionInvokeMethod = true
                }
            }
            if (functionReference.reflectionTargetSymbol != null) {
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

            functionReferenceClass.addFakeOverrides(
                    context.typeSystem,
                    // Built function overrides originalSuperMethod, while, if parent class is already lowered, it would
                    // transformedSuperMethod in its declaration list. We need not fake override in that case.
                    // Later lowerings will fix it and replace function with one overriding transformedSuperMethod.
                    ignoredParentSymbols = listOf(functionReference.overriddenFunctionSymbol)
            )
            return functionReferenceClass
        }

        fun build(): BuiltFunctionReference {
            val clazz = buildClass()
            val constrCall = irBuilder.irCallConstructor(clazz.primaryConstructor!!.symbol, emptyList()).apply {
                for ((index, value) in functionReference.boundValues.withIndex()) {
                    arguments[index] = value
                }
            }
            return BuiltFunctionReference(clazz, constrCall)
        }

        fun IrBuilderWithScope.irKFunctionDescription(description: KFunctionDescription): IrConstantValue {
            val kTypeGenerator = toNativeConstantReflectionBuilder(this@FunctionReferenceBuilder.context.ir.symbols)

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

        private fun buildInvokeMethod(
                superFunction: IrSimpleFunction,
                superInterfaceType: IrType,
                invokeFunction: IrSimpleFunction,
                boundFields: List<IrField>
        ) = functionReferenceClass.addFunction {
            startOffset = this@FunctionReferenceBuilder.startOffset
            endOffset = this@FunctionReferenceBuilder.endOffset
            origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
            name = superFunction.name
            returnType = invokeFunction.returnType
            isSuspend = superFunction.isSuspend
        }.apply {
            attributeOwnerId = functionReference.attributeOwnerId

            parameters += createDispatchReceiverParameterWithClassParent()
            require(superFunction.typeParameters.isEmpty()) { "Fun interface abstract function can't have type parameters" }

            val typeSubstitutor = IrTypeSubstitutor(
                    extractTypeParameters(superInterfaceType.classOrFail.owner).map { it.symbol },
                    (superInterfaceType as IrSimpleType).arguments,
                    allowEmptySubstitution = true
            )

            val nonDispatchParameters = superFunction.nonDispatchParameters.map {
                it.copyTo(this, type = typeSubstitutor.substitute(it.type), defaultValue = null)
            }
            this.parameters += nonDispatchParameters
            overriddenSymbols += superFunction.symbol

            val builder = context.createIrBuilder(symbol)
            body = builder.irBlockBody {
                val variablesMapping = buildMap {
                    for ((index, field) in boundFields.withIndex()) {
                        put(invokeFunction.parameters[index], irTemporary(irGetField(irGet(dispatchReceiverParameter!!), field)))
                    }
                    for ((index, parameter) in nonDispatchParameters.withIndex()) {
                        val invokeParameter = invokeFunction.parameters[index + boundFields.size]
                        if (parameter.type != invokeParameter.type) {
                            put(invokeParameter, irTemporary(irGet(parameter).implicitCastTo(invokeParameter.type)))
                        } else {
                            put(invokeParameter, parameter)
                        }
                    }
                }
                val transformedBody = invokeFunction.body!!.transform(object : VariableRemapper(variablesMapping) {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        if (expression.returnTargetSymbol == invokeFunction.symbol) {
                            expression.returnTargetSymbol = this@apply.symbol
                        }
                        return super.visitReturn(expression)
                    }

                    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                        if (declaration.parent == invokeFunction)
                            declaration.parent = this@apply
                        return super.visitDeclaration(declaration)
                    }
                }, null)
                when (transformedBody) {
                    is IrBlockBody -> +transformedBody.statements
                    is IrExpressionBody -> +irReturn(transformedBody.expression)
                    else -> error("Unexpected body type: ${transformedBody::class.simpleName}")
                }
            }
        }
    }
}
