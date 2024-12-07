/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

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

            // Handle SAM conversions which wrap a function reference:
            //     class sam$n(private val receiver: R) : Interface { override fun method(...) = receiver.target(...) }
            //
            // This avoids materializing an invokable KFunction representing, thus producing one less class.
            // This is actually very common, as `Interface { something }` is a local function + a SAM-conversion
            // of a reference to it into an implementation.
            override fun visitTypeOperator(expression: IrTypeOperatorCall, data: IrDeclarationParent): IrExpression {
                if (expression.operator == IrTypeOperator.SAM_CONVERSION) {
                    val invokable = expression.argument
                    if (invokable is IrFunctionReference) {
                        invokable.transformChildren(this, data)
                        return transformFunctionReference(invokable, data, expression.typeOperand)
                    }
                    if (invokable is IrBlock && invokable.origin.isLambda
                            && invokable.statements.last() is IrFunctionReference) {
                        invokable.statements.dropLast(1).forEach { it.transform(this, data) }
                        val reference = invokable.statements.last() as IrFunctionReference
                        reference.transformChildren(this, data)
                        invokable.statements[invokable.statements.lastIndex] = transformFunctionReference(reference, data, expression.typeOperand)
                        invokable.type = expression.type
                        return invokable
                    }
                }
                return super.visitTypeOperator(expression, data)
            }

            override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent): IrExpression {
                expression.transformChildren(this, data)

                for (i in stack.size - 1 downTo 0) {
                    val cur = stack[i]
                    if (cur is IrBlock)
                        continue
                    if (cur !is IrCall)
                        break
                    val argument = if (i < stack.size - 1) stack[i + 1] else expression
                    val parameter = cur.symbol.owner.valueParameters.singleOrNull {
                        cur.getValueArgument(it.indexInOldValueParameters) == argument
                    }
                    if (parameter?.annotations?.findAnnotation(VOLATILE_LAMBDA_FQ_NAME) != null) {
                        return IrRawFunctionReferenceImpl(
                                expression.startOffset,
                                expression.endOffset,
                                expression.type,
                                expression.symbol
                        )
                    }
                    break
                }

                if (!expression.type.isFunction() && !expression.type.isKFunction() &&
                        !expression.type.isKSuspendFunction() && !expression.type.isSuspendFunction()) {
                    // Not a subject of this lowering.
                    return expression
                }

                return transformFunctionReference(expression, data)
            }

            fun transformFunctionReference(expression: IrFunctionReference, parent: IrDeclarationParent, samSuperType: IrType? = null): IrExpression {
                val irBuilder = generationState.context.createIrBuilder((parent as IrSymbolOwner).symbol,
                        expression.startOffset, expression.endOffset)
                val (clazz, newExpression) = FunctionReferenceBuilder(irFile, parent, expression, generationState, irBuilder, samSuperType).build()
                return irBuilder.irBlock {
                    +clazz
                    +newExpression
                }
            }
        }, data = irFile)
    }

    private val VOLATILE_LAMBDA_FQ_NAME = FqName.fromSegments(listOf("kotlin", "native", "internal", "VolatileLambda"))

    private class FunctionReferenceBuilder(
            val irFile: IrFile,
            val parent: IrDeclarationParent,
            val functionReference: IrFunctionReference,
            val generationState: NativeGenerationState,
            val irBuilder: IrBuilderWithScope,
            val samSuperType: IrType? = null,
    ) {
        data class BuiltFunctionReference(val functionReferenceClass: IrClass, val functionReferenceExpression: IrExpression)

        private val context = generationState.context
        private val irBuiltIns = context.irBuiltIns
        private val symbols = context.ir.symbols
        private val irFactory = context.irFactory

        private val startOffset = functionReference.startOffset
        private val endOffset = functionReference.endOffset
        private val referencedFunction = functionReference.symbol.owner
        private val functionParameters = referencedFunction.parameters
        private val boundFunctionParameters = functionReference.getArgumentsWithIr().map { it.first }
        private val unboundFunctionParameters = functionParameters - boundFunctionParameters

        private val isLambda = functionReference.origin.isLambda
        private val isK = functionReference.type.isKFunction() || functionReference.type.isKSuspendFunction()
        private val isSuspend = functionReference.type.isSuspendFunction() || functionReference.type.isKSuspendFunction()
        private val adaptedReferenceOriginalTarget = functionReference.reflectionTarget?.owner
        private val functionReferenceTarget = adaptedReferenceOriginalTarget ?: referencedFunction

        /**
         * The first element of a pair is a type parameter of [referencedFunction], the second element is its argument in
         * [functionReference].
         */
        private val allTypeParametersAndArguments: List<Pair<IrTypeParameterSymbol, IrType>> =
                referencedFunction.typeParameters.map { typeParam ->
                    typeParam.symbol to functionReference.getTypeArgument(typeParam.index)!!
                }

        /**
         * @see allTypeParametersAndArguments
         */
        private val allTypeParametersAndArgumentsMap: Map<IrTypeParameterSymbol, IrType> = allTypeParametersAndArguments.toMap()

        /**
         * The distinct type arguments of [functionReference] that are not concrete types,
         * but are themselves type parameters coming from an enclosing scope.
         */
        private val typeParametersFromEnclosingScope: List<IrTypeParameter> = allTypeParametersAndArguments
                .mapNotNull { (_, typeArgument) -> (typeArgument.classifierOrNull as? IrTypeParameterSymbol)?.owner }.distinct()

        private val functionReferenceClass = irFactory.buildClass {
            startOffset = this@FunctionReferenceBuilder.startOffset
            endOffset = this@FunctionReferenceBuilder.endOffset
            origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
            val reflectionTarget = (functionReference.reflectionTarget ?: functionReference.symbol).owner
            if (isLambda) {
                name = SpecialNames.NO_NAME_PROVIDED
            } else {
                name = generationState.fileLowerState.getFunctionReferenceImplUniqueName("FUNCTION_REFERENCE_FOR$${reflectionTarget.name}$").synthesizedName
            }
            visibility = DescriptorVisibilities.LOCAL
        }.apply {
            hasSyntheticNameToBeHiddenInReflection = true
            parent = this@FunctionReferenceBuilder.parent

            // The function reference class only needs to be generic over type parameters coming from an enclosing scope.
            copyTypeParameters(typeParametersFromEnclosingScope)
            createThisReceiverParameter()
        }

        /**
         * Remaps [typeParametersFromEnclosingScope] to type parameters of [functionReferenceClass].
         */
        private val typeParameterRemapper = IrTypeParameterRemapper(
                typeParametersFromEnclosingScope.zip(functionReferenceClass.typeParameters).toMap()
        )

        private fun IrType.remappedTypeArguments(): List<IrType> {
            if (this !is IrSimpleType) return emptyList()
            return arguments.mapIndexed { index, typeArgument ->
                when (typeArgument) {
                    is IrTypeProjection -> typeParameterRemapper.remapType(typeArgument.type)
                    is IrStarProjection -> (classifier as IrClassSymbol).owner.typeParameters[index].defaultType.erasure()
                }
            }
        }

        private val functionParameterAndReturnTypes = functionReference.type.remappedTypeArguments()

        private val functionParameterTypes = functionParameterAndReturnTypes.dropLast(1)
        private val functionReturnType = functionParameterAndReturnTypes.last()

        private val functionReferenceThis = functionReferenceClass.thisReceiver!!

        /**
         * Replaces [typeParameter] of [referencedFunction] with the corresponding type parameter of [functionReferenceClass]
         * if such correspondence takes place. Otherwise, just returns [typeParameter] as [IrType].
         */
        private fun substituteTypeParameterOfReferencedFunction(typeParameter: IrTypeParameter): IrType {
            if (typeParameter.parent != referencedFunction) {
                // TODO: We might have references to off-scope type parameters (because of the inliner, see KT-56500 for details).
                // Fixing inliner requires a lot of work, so just return the upper bound for now instead of throwing an error.
                // compilationException(
                //         "The type parameter ${typeParameter.render()} is not defined in the referenced function ${referencedFunction.render()}",
                //         functionReference
                // )
                return typeParameter.erasedUpperBound.defaultType
            }
            return typeParameterRemapper.remapType(allTypeParametersAndArguments[typeParameter.index].second)
        }

        /**
         * Substitutes a bound value parameter's [type] with a new type according to the following rules:
         *
         * - If [type] is a type parameter from an enclosing scope (i.e. for which we don't know the concrete type), replace it with
         *   the corresponding [functionReferenceClass]'s type parameter.
         * - If this value parameter's type is a type parameter for which we know the concrete type, replace it with
         *   the concrete type. For example, consider the `5::foo` function reference where `foo` is declared as `fun <T> T.foo()`. Here,
         *   `T` will be replaced with `Int`.
         * - Otherwise, just return [type] itself.
         */
        private fun substituteBoundValueParameterType(type: IrType): IrType =
                ((type.classifierOrNull as? IrTypeParameterSymbol)?.owner?.let(this::substituteTypeParameterOfReferencedFunction) ?: type)
                        .substitute(allTypeParametersAndArgumentsMap)

        private val argumentToPropertiesMap = boundFunctionParameters.associateWith {
            functionReferenceClass.addField {
                startOffset = this@FunctionReferenceBuilder.startOffset
                endOffset = this@FunctionReferenceBuilder.endOffset
                origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
                name = it.name
                type = substituteBoundValueParameterType(it.type)
                isFinal = true
            }
        }

        private val kFunctionImplSymbol = symbols.kFunctionImpl
        private val kFunctionDescriptionSymbol = symbols.kFunctionDescription
        private val kFunctionImplConstructorSymbol = kFunctionImplSymbol.constructors.single()
        private val kSuspendFunctionImplSymbol = symbols.kSuspendFunctionImpl
        private val kSuspendFunctionImplConstructorSymbol = kSuspendFunctionImplSymbol.constructors.single()

        private fun buildClass(): IrClass {
            if (unboundFunctionParameters.size != (functionReference.type as IrSimpleType).arguments.size - 1) {
                compilationException(
                        "The number of unbound value parameters of the function reference should match the number of type arguments " +
                                "of the K[Suspend]FunctionN superclass minus one.\n\n" +
                                "Unbound function parameters:\n" +
                                unboundFunctionParameters.joinToString(separator = "\n", transform = IrElement::render) +
                                "\n\nFunction reference type: " +
                                functionReference.type.render(),
                        functionReference
                )
            }
            val superClass = when {
                isLambda -> irBuiltIns.anyType
                isSuspend -> kSuspendFunctionImplSymbol.typeWith(functionReturnType)
                else -> kFunctionImplSymbol.typeWith(functionReturnType)
            }
            val superTypes = mutableListOf(superClass)
            val transformedSuperMethod: IrSimpleFunction
            if (samSuperType != null) {
                val remappedSuperType = (samSuperType.classOrNull ?: error("Expected a class but was: ${samSuperType.render()}"))
                        .typeWith(samSuperType.remappedTypeArguments())
                superTypes += remappedSuperType
                transformedSuperMethod = remappedSuperType.classOrNull!!.functions.single {
                    val function = it.owner
                    function.modality == Modality.ABSTRACT && function.origin !is DECLARATION_ORIGIN_BRIDGE_METHOD
                }.owner
            } else {
                val numberOfParameters = unboundFunctionParameters.size
                if (isSuspend) {
                    val suspendFunctionClass = (if (isK) symbols.kSuspendFunctionN(numberOfParameters) else symbols.suspendFunctionN(numberOfParameters)).owner
                    superTypes += suspendFunctionClass.typeWith(functionParameterAndReturnTypes)
                    transformedSuperMethod = suspendFunctionClass.invokeFun!!
                } else {
                    val functionClass = (if (isK) symbols.kFunctionN(numberOfParameters) else symbols.functionN(numberOfParameters)).owner
                    superTypes += functionClass.typeWith(functionParameterAndReturnTypes)
                    transformedSuperMethod = functionClass.invokeFun!!
                }
            }
            val originalSuperMethod = transformedSuperMethod.suspendFunction ?: transformedSuperMethod
            buildInvokeMethod(originalSuperMethod)

            functionReferenceClass.superTypes += superTypes
            if (!isLambda) {
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
                    function.createDispatchReceiverParameter()
                    function.overriddenSymbols += overridden
                    function.body = context.createIrBuilder(function.symbol, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET).irBlockBody {
                        +irReturn(
                                value(function)
                        )
                    }
                }

                fun addOverride(name: String, value: IrBuilderWithScope.() -> IrExpression) {
                    addOverrideInner(name) { _ -> value() }
                }


                listOfNotNull(
                        functionReference.symbol.owner.dispatchReceiverParameter,
                        functionReference.symbol.owner.extensionReceiverParameter
                ).singleOrNull { it in boundFunctionParameters }
                        ?.let { receiver ->
                            addOverrideInner("computeReceiver") { f ->
                                irGetField(irGet(f.dispatchReceiverParameter!!), argumentToPropertiesMap[receiver]!!)
                            }
                        }
            }

            functionReferenceClass.addFakeOverrides(
                    context.typeSystem,
                    // Built function overrides originalSuperMethod, while, if parent class is already lowered, it would
                    // transformedSuperMethod in its declaration list. We need not fake override in that case.
                    // Later lowerings will fix it and replace function with one overriding transformedSuperMethod.
                    ignoredParentSymbols = listOf(transformedSuperMethod.symbol)
            )

            functionReferenceClass.remapTypes(typeParameterRemapper)

            return functionReferenceClass
        }

        private fun buildConstructor() = functionReferenceClass.addConstructor {
            startOffset = this@FunctionReferenceBuilder.startOffset
            endOffset = this@FunctionReferenceBuilder.endOffset
            origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
            isPrimary = true
        }.apply {
            valueParameters += boundFunctionParameters.mapIndexed { index, parameter ->
                parameter.copyTo(
                        this,
                        DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        type = substituteBoundValueParameterType(parameter.type)
                )
            }

            body = context.createIrBuilder(symbol, startOffset, endOffset).irBlockBody {
                val superConstructor = when {
                    isLambda -> irBuiltIns.anyClass.owner.constructors.single()
                    this@FunctionReferenceBuilder.isSuspend -> kSuspendFunctionImplConstructorSymbol.owner
                    else -> kFunctionImplConstructorSymbol.owner
                }
                +irDelegatingConstructorCall(superConstructor).apply {
                    if (!isLambda) {
                        putValueArgument(0, getDescription())
                    }
                }
                +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, irBuiltIns.unitType)
                // Save all arguments to fields.
                boundFunctionParameters.forEachIndexed { index, parameter ->
                    +irSetField(irGet(functionReferenceThis), argumentToPropertiesMap[parameter]!!, irGet(valueParameters[index]))
                }
            }
        }

        fun build(): BuiltFunctionReference {
            val clazz = buildClass()
            val constructor = buildConstructor()
            val arguments = functionReference.getArgumentsWithIr()
            val typeArguments = typeParametersFromEnclosingScope.map { it.defaultType }
            val constrCall = irBuilder.irCallConstructor(constructor.symbol, typeArguments).apply {
                arguments.forEachIndexed { index, argument ->
                    putValueArgument(index, argument.second)
                }
            }

            return BuiltFunctionReference(clazz, constrCall)
        }

        private fun IrBuilderWithScope.getDescription() : IrConstantValue {
            val kTypeGenerator = toNativeConstantReflectionBuilder(this@FunctionReferenceBuilder.context.ir.symbols)

            return irConstantObject(
                    kFunctionDescriptionSymbol.owner,
                    mapOf(
                            "flags" to irConstantPrimitive(irInt(getFlags())),
                            "arity" to irConstantPrimitive(irInt(getArity())),
                            "fqName" to irConstantPrimitive(irString(getFqName())),
                            "name" to irConstantPrimitive(irString(getName().asString())),
                            "returnType" to kTypeGenerator.irKType(referencedFunction.returnType)
                    )
            )
        }

        // this value is used only for hashCode and equals, to distinguish different wrappers on same functions
        private fun getFlags() =
                listOfNotNull(
                        (1 shl 0).takeIf { referencedFunction.isSuspend },
                        (1 shl 1).takeIf { hasVarargMappedToElement() },
                        (1 shl 2).takeIf { isSuspendConversion() },
                        (1 shl 3).takeIf { isCoercedToUnit() },
                        (1 shl 4).takeIf { isFunInterfaceConstructorAdapter() }
                ).sum()

        private fun getFqName() =
                if (isFunInterfaceConstructorAdapter())
                    referencedFunction.returnType.getClass()!!.fqNameForIrSerialization.toString()
                else
                    functionReferenceTarget.computeFullName()

        private fun getName() =
                ((functionReferenceTarget as? IrSimpleFunction)?.attributeOwnerId as? IrSimpleFunction)?.name
                        ?: functionReferenceTarget.name

        private fun getArity() = unboundFunctionParameters.size + if (functionReferenceTarget.isSuspend) 1 else 0

        private fun isFunInterfaceConstructorAdapter() =
                referencedFunction.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR

        private fun isCoercedToUnit() =
                adaptedReferenceOriginalTarget?.returnType?.isUnit() == false && referencedFunction.returnType.isUnit()

        private fun isSuspendConversion() =
                adaptedReferenceOriginalTarget?.isSuspend == false && referencedFunction.isSuspend

        private fun hasVarargMappedToElement(): Boolean {
            if (adaptedReferenceOriginalTarget == null) return false
            val originalParameters = adaptedReferenceOriginalTarget.allParameters
            val adaptedParameters = functionReference.symbol.owner.allParameters
            var index = 0
            // TODO: There should be similar code somewhere in the resolve.
            while (index < originalParameters.size && index < adaptedParameters.size) {
                val originalParameter = originalParameters[index]
                val adaptedParameter = adaptedParameters[index]
                if (originalParameter.defaultValue != null) return false
                if (originalParameter.isVararg) {
                    if (originalParameter.varargElementType!!.erasure() == adaptedParameter.type.erasure())
                        return true
                }
                ++index
            }
            return false
        }

        private fun buildInvokeMethod(superFunction: IrSimpleFunction) = functionReferenceClass.addFunction {
            startOffset = this@FunctionReferenceBuilder.startOffset
            endOffset = this@FunctionReferenceBuilder.endOffset
            origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
            name = superFunction.name
            returnType = functionReturnType
            isSuspend = superFunction.isSuspend
        }.apply {
            attributeOwnerId = functionReference.attributeOwnerId
            val function = this

            function.createDispatchReceiverParameter()

            extensionReceiverParameter = superFunction.extensionReceiverParameter?.copyTo(function)

            overriddenSymbols += superFunction.symbol

            val valueParameters = mutableListOf<IrValueParameter>()

            body = context.createIrBuilder(function.symbol, startOffset, endOffset).irBlockBody(startOffset, endOffset) {
                +irReturn(
                        irCall(referencedFunction).apply {
                            var unboundIndex = 0
                            val unboundArgsSet = unboundFunctionParameters.toSet()
                            for (parameter in functionParameters) {
                                val argument =
                                        if (!unboundArgsSet.contains(parameter))
                                        // Bound parameter - read from field.
                                            irGetField(
                                                    irGet(function.dispatchReceiverParameter!!),
                                                    argumentToPropertiesMap[parameter]!!
                                            )
                                        else {
                                            if (parameter == referencedFunction.extensionReceiverParameter
                                                    && extensionReceiverParameter != null)
                                                irGet(extensionReceiverParameter!!)
                                            else {
                                                // Sometimes FE resolves type as an intersection type which gets approximated to Nothing,
                                                // so try to get the type from the referenced function in such a case.
                                                // NOTE: the JVM counterpart of this lowering takes all the types from the referenced function
                                                // for lambdas and SAM conversions, but it can't be done here because of the different
                                                // lowerings order (in K/N the LocalDeclarationsLowering has already been applied by this point,
                                                // and lambdas might have their own type parameters, copied from the outer scopes, and it
                                                // gets really messy to try to substitute them to the correct ones).
                                                val valueParameter = if ((samSuperType != null || isLambda)
                                                        && functionParameterTypes[unboundIndex].isNothing()
                                                ) {
                                                    parameter.copyTo(
                                                            function, DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                                                            type = parameter.type)
                                                } else {
                                                    superFunction.valueParameters[unboundIndex].copyTo(
                                                            function, DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                                                            type = functionParameterTypes[unboundIndex])
                                                }
                                                ++unboundIndex
                                                valueParameters.add(valueParameter)
                                                irGet(valueParameter)
                                            }
                                        }
                                when (parameter) {
                                    referencedFunction.dispatchReceiverParameter -> dispatchReceiver = argument
                                    referencedFunction.extensionReceiverParameter -> extensionReceiver = argument
                                    else -> putValueArgument(parameter.indexInOldValueParameters, argument)
                                }
                            }
                            assert(unboundIndex == valueParameters.size) { "Not all arguments of <invoke> are used" }

                            referencedFunction.typeParameters.forEach { typeParam ->
                                putTypeArgument(typeParam.index, substituteTypeParameterOfReferencedFunction(typeParam))
                            }
                        }
                )
            }

            this.valueParameters = valueParameters
        }
    }
}
