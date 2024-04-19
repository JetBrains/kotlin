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

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeCallableIds
import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.FeatureFlag
import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.analysis.knownStable
import androidx.compose.compiler.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.codegen.anyTypeArgument
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isSuspendFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm

private class CaptureCollector {
    val captures = mutableSetOf<IrValueDeclaration>()
    val capturedDeclarations = mutableSetOf<IrSymbolOwner>()
    val hasCaptures: Boolean get() = captures.isNotEmpty() || capturedDeclarations.isNotEmpty()

    fun recordCapture(local: IrValueDeclaration) {
        captures.add(local)
    }

    fun recordCapture(local: IrSymbolOwner) {
        capturedDeclarations.add(local)
    }
}

private abstract class DeclarationContext {
    val localDeclarationCaptures = mutableMapOf<IrSymbolOwner, Set<IrValueDeclaration>>()
    fun recordLocalDeclaration(local: DeclarationContext) {
        localDeclarationCaptures[local.declaration] = local.captures
    }

    abstract val composable: Boolean
    abstract val symbol: IrSymbol
    abstract val declaration: IrSymbolOwner
    abstract val captures: Set<IrValueDeclaration>
    abstract val functionContext: FunctionContext?
    abstract fun declareLocal(local: IrValueDeclaration?)
    abstract fun recordCapture(local: IrValueDeclaration?): Boolean
    abstract fun recordCapture(local: IrSymbolOwner?)
    abstract fun pushCollector(collector: CaptureCollector)
    abstract fun popCollector(collector: CaptureCollector)
}

private fun List<DeclarationContext>.recordCapture(value: IrValueDeclaration) {
    for (dec in reversed()) {
        val shouldBreak = dec.recordCapture(value)
        if (shouldBreak) break
    }
}

private fun List<DeclarationContext>.recordLocalDeclaration(local: DeclarationContext) {
    for (dec in reversed()) {
        dec.recordLocalDeclaration(local)
    }
}

private fun List<DeclarationContext>.recordLocalCapture(
    local: IrSymbolOwner
): Set<IrValueDeclaration>? {
    val capturesForLocal = reversed().firstNotNullOfOrNull { it.localDeclarationCaptures[local] }
    if (capturesForLocal != null) {
        capturesForLocal.forEach { recordCapture(it) }
        for (dec in reversed()) {
            dec.recordCapture(local)
            if (dec.localDeclarationCaptures.containsKey(local)) {
                // this is the scope that the class was defined in, so above this we don't need
                // to do anything
                break
            }
        }
    }
    return capturesForLocal
}

private class SymbolOwnerContext(override val declaration: IrSymbolOwner) : DeclarationContext() {
    override val composable get() = false
    override val functionContext: FunctionContext? get() = null
    override val symbol get() = declaration.symbol
    override val captures: Set<IrValueDeclaration> get() = emptySet()
    override fun declareLocal(local: IrValueDeclaration?) {}
    override fun recordCapture(local: IrValueDeclaration?): Boolean {
        return false
    }

    override fun recordCapture(local: IrSymbolOwner?) {}
    override fun pushCollector(collector: CaptureCollector) {}
    override fun popCollector(collector: CaptureCollector) {}
}

private class FunctionLocalSymbol(
    override val declaration: IrSymbolOwner,
    override val functionContext: FunctionContext
) : DeclarationContext() {
    override val composable: Boolean get() = functionContext.composable
    override val symbol: IrSymbol get() = declaration.symbol
    override val captures: Set<IrValueDeclaration> get() = functionContext.captures
    override fun declareLocal(local: IrValueDeclaration?) = functionContext.declareLocal(local)
    override fun recordCapture(local: IrValueDeclaration?) = functionContext.recordCapture(local)
    override fun recordCapture(local: IrSymbolOwner?) = functionContext.recordCapture(local)
    override fun pushCollector(collector: CaptureCollector) =
        functionContext.pushCollector(collector)

    override fun popCollector(collector: CaptureCollector) =
        functionContext.popCollector(collector)
}

private class FunctionContext(
    override val declaration: IrFunction,
    override val composable: Boolean,
    val canRemember: Boolean
) : DeclarationContext() {
    override val symbol get() = declaration.symbol
    override val functionContext: FunctionContext get() = this
    val locals = mutableSetOf<IrValueDeclaration>()
    override val captures: MutableSet<IrValueDeclaration> = mutableSetOf()
    var collectors = mutableListOf<CaptureCollector>()

    init {
        declaration.valueParameters.forEach {
            declareLocal(it)
        }
        declaration.dispatchReceiverParameter?.let { declareLocal(it) }
        declaration.extensionReceiverParameter?.let { declareLocal(it) }
    }

    override fun declareLocal(local: IrValueDeclaration?) {
        if (local != null) {
            locals.add(local)
        }
    }

    override fun recordCapture(local: IrValueDeclaration?): Boolean {
        val containsLocal = locals.contains(local)
        if (local != null && collectors.isNotEmpty() && containsLocal) {
            for (collector in collectors) {
                collector.recordCapture(local)
            }
        }
        if (local != null && declaration.isLocal && !containsLocal) {
            captures.add(local)
        }
        return containsLocal
    }

    override fun recordCapture(local: IrSymbolOwner?) {
        if (local != null) {
            val captures = localDeclarationCaptures[local]
            for (collector in collectors) {
                collector.recordCapture(local)
                if (captures != null) {
                    for (capture in captures) {
                        collector.recordCapture(capture)
                    }
                }
            }
        }
    }

    override fun pushCollector(collector: CaptureCollector) {
        collectors.add(collector)
    }

    override fun popCollector(collector: CaptureCollector) {
        require(collectors.lastOrNull() == collector)
        collectors.removeAt(collectors.size - 1)
    }
}

private class ClassContext(override val declaration: IrClass) : DeclarationContext() {
    override val composable: Boolean = false
    override val symbol get() = declaration.symbol
    override val functionContext: FunctionContext? = null
    override val captures: MutableSet<IrValueDeclaration> = mutableSetOf()
    val thisParam: IrValueDeclaration? = declaration.thisReceiver!!
    var collectors = mutableListOf<CaptureCollector>()
    override fun declareLocal(local: IrValueDeclaration?) {}
    override fun recordCapture(local: IrValueDeclaration?): Boolean {
        val isThis = local == thisParam
        val isConstructorParam = (local?.parent as? IrConstructor)?.parent === declaration
        val isClassParam = isThis || isConstructorParam
        if (local != null && collectors.isNotEmpty() && isClassParam) {
            for (collector in collectors) {
                collector.recordCapture(local)
            }
        }
        if (local != null && declaration.isLocal && !isClassParam) {
            captures.add(local)
        }
        return isClassParam
    }

    override fun recordCapture(local: IrSymbolOwner?) {}
    override fun pushCollector(collector: CaptureCollector) {
        collectors.add(collector)
    }

    override fun popCollector(collector: CaptureCollector) {
        require(collectors.lastOrNull() == collector)
        collectors.removeAt(collectors.size - 1)
    }
}

class ComposerLambdaMemoization(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    featureFlags: FeatureFlags,
) : AbstractComposeLowering(context, symbolRemapper, metrics, stabilityInferencer, featureFlags),

    ModuleLoweringPass {

    private val declarationContextStack = mutableListOf<DeclarationContext>()

    private val currentFunctionContext: FunctionContext?
        get() =
            declarationContextStack.peek()?.functionContext

    private var composableSingletonsClass: IrClass? = null
    private var currentFile: IrFile? = null

    private var inlineLambdaInfo = ComposeInlineLambdaLocator(context)

    private val rememberFunctions =
        getTopLevelFunctions(ComposeCallableIds.remember).map { it.owner }

    private val composableLambdaFunction by guardedLazy {
        getTopLevelFunction(ComposeCallableIds.composableLambda)
    }

    private val composableLambdaNFunction by guardedLazy {
        getTopLevelFunction(ComposeCallableIds.composableLambdaN)
    }

    private val composableLambdaInstanceFunction by guardedLazy {
        getTopLevelFunction(ComposeCallableIds.composableLambdaInstance)
    }

    private val composableLambdaInstanceNFunction by guardedLazy {
        getTopLevelFunction(ComposeCallableIds.composableLambdaNInstance)
    }

    private val rememberComposableLambdaFunction by guardedLazy {
        getTopLevelFunctions(ComposeCallableIds.rememberComposableLambda).singleOrNull()
    }

    private val rememberComposableLambdaNFunction by guardedLazy {
        getTopLevelFunctions(ComposeCallableIds.rememberComposableLambdaN).singleOrNull()
    }

    private val useNonSkippingGroupOptimization by guardedLazy {
        // Uses `rememberComposableLambda` as a indication that the runtime supports
        // generating remember after call as it was added at the same time as the slot table was
        // modified to support remember after call.
        FeatureFlag.OptimizeNonSkippingGroups.enabled && rememberComposableLambdaFunction != null
    }

    private fun getOrCreateComposableSingletonsClass(): IrClass {
        if (composableSingletonsClass != null) return composableSingletonsClass!!
        val declaration = currentFile!!
        val filePath = declaration.fileEntry.name
        val fileName = filePath.split('/').last()
        val current = context.irFactory.buildClass {
            startOffset = SYNTHETIC_OFFSET
            endOffset = SYNTHETIC_OFFSET
            kind = ClassKind.OBJECT
            visibility = DescriptorVisibilities.INTERNAL
            val shortName = PackagePartClassUtils.getFilePartShortName(fileName)
            // the name of the LiveLiterals class is per-file, so we use the same name that
            // the kotlin file class lowering produces, prefixed with `LiveLiterals$`.
            name = Name.identifier("ComposableSingletons${"$"}$shortName")
        }.also {
            it.createParameterDeclarations()

            // store the full file path to the file that this class is associated with in an
            // annotation on the class. This will be used by tooling to associate the keys
            // inside of this class with actual PSI in the editor.
            it.addConstructor {
                isPrimary = true
            }.also { constructor ->
                constructor.body = DeclarationIrBuilder(context, it.symbol).irBlockBody {
                    +irDelegatingConstructorCall(
                        context
                            .irBuiltIns
                            .anyClass
                            .owner
                            .primaryConstructor!!
                    )
                    +IrInstanceInitializerCallImpl(
                        startOffset = this.startOffset,
                        endOffset = this.endOffset,
                        classSymbol = it.symbol,
                        type = it.defaultType
                    )
                }
            }
        }.markAsComposableSingletonClass()
        composableSingletonsClass = current
        return current
    }

    override fun visitFile(declaration: IrFile): IrFile {
        includeFileNameInExceptionTrace(declaration) {
            val prevFile = currentFile
            val prevClass = composableSingletonsClass
            try {
                currentFile = declaration
                composableSingletonsClass = null
                val file = super.visitFile(declaration)
                // if there were no constants found in the entire file, then we don't need to
                // create this class at all
                val resultingClass = composableSingletonsClass
                if (resultingClass != null && resultingClass.declarations.isNotEmpty()) {
                    file.addChild(resultingClass)
                }
                return file
            } finally {
                currentFile = prevFile
                composableSingletonsClass = prevClass
            }
        }
    }

    override fun lower(module: IrModuleFragment) {
        inlineLambdaInfo.scan(module)
        module.transformChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration is IrFunction)
            return super.visitDeclaration(declaration)

        val functionContext = currentFunctionContext
        if (functionContext != null) {
            declarationContextStack.push(FunctionLocalSymbol(declaration, functionContext))
        } else {
            declarationContextStack.push(SymbolOwnerContext(declaration))
        }
        val result = super.visitDeclaration(declaration)
        declarationContextStack.pop()
        return result
    }

    private fun irCurrentComposer(): IrExpression {
        val currentComposerSymbol = getTopLevelPropertyGetter(ComposeCallableIds.currentComposer)

        return IrCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            composerIrClass.defaultType.replaceArgumentsWithStarProjections(),
            currentComposerSymbol as IrSimpleFunctionSymbol,
            currentComposerSymbol.owner.typeParameters.size,
            currentComposerSymbol.owner.valueParameters.size,
            IrStatementOrigin.FOR_LOOP_ITERATOR,
        )
    }

    private val IrFunction.allowsComposableCalls: Boolean
        get() = hasComposableAnnotation() ||
            inlineLambdaInfo.preservesComposableScope(this) &&
            declarationContextStack.peek()?.composable == true

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val composable = declaration.allowsComposableCalls
        val canRemember = composable &&
            // Don't use remember in an inline function
            !declaration.isInline

        val context = FunctionContext(declaration, composable, canRemember)
        if (declaration.isLocal) {
            declarationContextStack.recordLocalDeclaration(context)
        }
        declarationContextStack.push(context)
        val result = super.visitFunction(declaration)
        declarationContextStack.pop()
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val context = ClassContext(declaration)
        if (declaration.isLocal) {
            declarationContextStack.recordLocalDeclaration(context)
        }
        declarationContextStack.push(context)
        val result = super.visitClass(declaration)
        declarationContextStack.pop()
        return result
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        declarationContextStack.peek()?.declareLocal(declaration)
        return super.visitVariable(declaration)
    }

    override fun visitValueAccess(expression: IrValueAccessExpression): IrExpression {
        declarationContextStack.recordCapture(expression.symbol.owner)
        return super.visitValueAccess(expression)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        val result = super.visitBlock(expression)

        if (result is IrBlock && result.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE) {
            if (inlineLambdaInfo.isInlineFunctionExpression(expression)) {
                // Do not memoize function references for inline lambdas
                return result
            }

            val functionReference = result.statements.last()
            if (functionReference !is IrFunctionReference) {
                //  Do not memoize if the expected shape doesn't match.
                return result
            }

            return rememberFunctionReference(functionReference, expression)
        }

        return result
    }

    // Memoize the instance created by using the :: operator
    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val result = super.visitFunctionReference(expression)

        if (
            inlineLambdaInfo.isInlineFunctionExpression(expression) ||
                inlineLambdaInfo.isInlineLambda(expression.symbol.owner)
        ) {
            // Do not memoize function references used in inline parameters.
            return result
        }

        if (expression.symbol.owner.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE) {
            // Adapted function reference (inexact function signature match) is handled in block
            return result
        }

        if (result !is IrFunctionReference) {
            // Do not memoize if the shape doesn't match
            return result
        }

        return rememberFunctionReference(result, result)
    }

    private fun rememberFunctionReference(
        reference: IrFunctionReference,
        expression: IrExpression
    ): IrExpression {
        // Get the local captures for local function ref, to make sure we invalidate memoized
        // reference if its capture is different.
        val localCaptures = if (reference.symbol.owner.visibility == DescriptorVisibilities.LOCAL) {
            declarationContextStack.recordLocalCapture(reference.symbol.owner)
        } else {
            null
        }
        val functionContext = currentFunctionContext ?: return expression

        // The syntax <expr>::<method>(<params>) and ::<function>(<params>) is reserved for
        // future use. Revisit implementation if this syntax is as a curry syntax in the future.
        // The most likely correct implementation is to treat the parameters exactly as the
        // receivers are treated below.

        // Do not attempt memoization if the referenced function has context receivers.
        if (reference.symbol.owner.contextReceiverParametersCount > 0) {
            return expression
        }

        // Do not attempt memoization if value parameters are not null. This is to guard against
        // unexpected IR shapes.
        for (i in 0 until reference.valueArgumentsCount) {
            if (reference.getValueArgument(i) != null) {
                return expression
            }
        }

        if (functionContext.canRemember) {
            // Memoize the reference for <expr>::<method>
            val dispatchReceiver = reference.dispatchReceiver
            val extensionReceiver = reference.extensionReceiver

            val hasReceiver = dispatchReceiver != null || extensionReceiver != null
            val receiverIsStable =
                dispatchReceiver.isNullOrStable() &&
                    extensionReceiver.isNullOrStable()

            val captures = mutableListOf<IrValueDeclaration>()
            if (localCaptures != null) {
                captures.addAll(localCaptures)
            }

            if (hasReceiver && (FeatureFlag.StrongSkipping.enabled || receiverIsStable)) {
                // Save the receivers into a temporaries and memoize the function reference using
                // the resulting temporaries
                val builder = DeclarationIrBuilder(
                    generatorContext = context,
                    symbol = functionContext.symbol,
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset
                )
                return builder.irBlock(
                    resultType = expression.type
                ) {
                    val tempDispatchReceiver = dispatchReceiver?.let {
                        val tmp = irTemporary(it)
                        captures.add(tmp)
                        tmp
                    }
                    val tempExtensionReceiver = extensionReceiver?.let {
                        val tmp = irTemporary(it)
                        captures.add(tmp)
                        tmp
                    }

                    // Patch reference receiver in place
                    reference.dispatchReceiver = tempDispatchReceiver?.let { irGet(it) }
                    reference.extensionReceiver = tempExtensionReceiver?.let { irGet(it) }

                    +rememberExpression(
                        functionContext,
                        expression,
                        captures
                    )
                }
            } else if (dispatchReceiver == null && extensionReceiver == null) {
                return rememberExpression(functionContext, expression, captures)
            }
        }

        return expression
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        // SAM conversions are handled by Kotlin compiler
        // We only need to make sure that remember is handled correctly around type operator
        if (
            expression.operator != IrTypeOperator.SAM_CONVERSION ||
            currentFunctionContext?.canRemember != true
        ) {
            return super.visitTypeOperator(expression)
        }

        // Unwrap function from type operator
        val originalFunctionExpression =
            expression.findSamFunctionExpr() ?: return super.visitTypeOperator(expression)

        // Record capture variables for this scope
        val collector = CaptureCollector()
        startCollector(collector)
        // Handle inside of the function expression
        val result = super.visitFunctionExpression(originalFunctionExpression)
        stopCollector(collector)

        // If the ancestor converted this then return
        val newFunctionExpression = result as? IrFunctionExpression ?: return result

        // Construct new type operator call to wrap remember around.
        val newArgument = when (val argument = expression.argument) {
            is IrFunctionExpression -> newFunctionExpression
            is IrTypeOperatorCall -> {
                require(
                    argument.operator == IrTypeOperator.IMPLICIT_CAST &&
                        argument.argument == originalFunctionExpression
                ) {
                    "Only implicit cast is supported inside SAM conversion"
                }
                IrTypeOperatorCallImpl(
                    argument.startOffset,
                    argument.endOffset,
                    argument.type,
                    argument.operator,
                    argument.typeOperand,
                    newFunctionExpression
                )
            }

            else -> error("Unknown ")
        }

        val expressionToRemember =
            IrTypeOperatorCallImpl(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                IrTypeOperator.SAM_CONVERSION,
                expression.typeOperand,
                newArgument
            )
        return rememberExpression(
            currentFunctionContext!!,
            expressionToRemember,
            collector.captures.toList()
        )
    }

    private fun visitNonComposableFunctionExpression(
        expression: IrFunctionExpression,
    ): IrExpression {
        val functionContext = currentFunctionContext
            ?: return super.visitFunctionExpression(expression)

        if (
        // Only memoize non-composable lambdas in a context we can use remember
            !functionContext.canRemember ||
            // Don't memoize inlined lambdas
            inlineLambdaInfo.isInlineLambda(expression.function)
        ) {
            return super.visitFunctionExpression(expression)
        }

        // Record capture variables for this scope
        val collector = CaptureCollector()
        startCollector(collector)
        // Wrap composable functions expressions or memoize non-composable function expressions
        val result = super.visitFunctionExpression(expression)
        stopCollector(collector)

        // If the ancestor converted this then return
        val functionExpression = result as? IrFunctionExpression ?: return result

        return rememberExpression(
            functionContext,
            functionExpression,
            collector.captures.toList()
        )
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val fn = expression.symbol.owner
        if (fn.isLocal) {
            declarationContextStack.recordLocalCapture(fn)
        }
        return super.visitCall(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val fn = expression.symbol.owner
        val cls = fn.parent as? IrClass
        if (cls != null && fn.isLocal) {
            declarationContextStack.recordLocalCapture(cls)
        }
        return super.visitConstructorCall(expression)
    }

    private fun visitComposableFunctionExpression(
        expression: IrFunctionExpression,
        declarationContext: DeclarationContext
    ): IrExpression {
        val collector = CaptureCollector()
        startCollector(collector)
        val result = super.visitFunctionExpression(expression)
        stopCollector(collector)

        // If the ancestor converted this then return
        val functionExpression = result as? IrFunctionExpression ?: return result

        // Do not wrap target of an inline function
        if (inlineLambdaInfo.isInlineLambda(expression.function)) {
            return functionExpression
        }

        // Do not wrap composable lambdas with return results
        if (!functionExpression.function.returnType.isUnit()) {
            metrics.recordLambda(
                composable = true,
                memoized = !collector.hasCaptures,
                singleton = !collector.hasCaptures
            )
            return functionExpression
        }

        val wrapped = wrapFunctionExpression(declarationContext, functionExpression, collector)

        metrics.recordLambda(
            composable = true,
            memoized = true,
            singleton = !collector.hasCaptures
        )

        if (!collector.hasCaptures) {
            if (!context.platform.isJvm() && hasTypeParameter(expression.type)) {
                // This is a workaround
                // for TypeParameters having initial parents (old IrFunctions before deepCopy).
                // Otherwise it doesn't compile on k/js and k/native (can't find symbols).
                // Ideally we will find a solution to remap symbols of TypeParameters in
                // ComposableSingletons properties after ComposerParamTransformer
                // (deepCopy in ComposerParamTransformer didn't help).
                return wrapped
            }
            return irGetComposableSingleton(
                lambdaExpression = wrapped,
                lambdaType = expression.type
            )
        } else {
            return wrapped
        }
    }

    private fun hasTypeParameter(type: IrType): Boolean {
        return type.anyTypeArgument { true }
    }

    private fun irGetComposableSingleton(
        lambdaExpression: IrExpression,
        lambdaType: IrType
    ): IrExpression {
        val clazz = getOrCreateComposableSingletonsClass()
        val lambdaName = "lambda-${clazz.declarations.size}"
        val lambdaProp = clazz.addProperty {
            name = Name.identifier(lambdaName)
            visibility = DescriptorVisibilities.INTERNAL
        }.also { p ->
            p.backingField = context.irFactory.buildField {
                startOffset = SYNTHETIC_OFFSET
                endOffset = SYNTHETIC_OFFSET
                name = Name.identifier(lambdaName)
                type = lambdaType
                visibility = DescriptorVisibilities.INTERNAL
                isStatic = context.platform.isJvm()
            }.also { f ->
                f.correspondingPropertySymbol = p.symbol
                f.parent = clazz
                f.initializer = DeclarationIrBuilder(context, clazz.symbol)
                    .irExprBody(lambdaExpression.markIsTransformedLambda())
            }
            p.addGetter {
                returnType = lambdaType
                visibility = DescriptorVisibilities.INTERNAL
                origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
            }.also { fn ->
                val thisParam = clazz.thisReceiver!!.copyTo(fn)
                fn.parent = clazz
                fn.dispatchReceiverParameter = thisParam
                fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                    +irReturn(irGetField(irGet(thisParam), p.backingField!!))
                }
            }
        }
        return irCall(
            lambdaProp.getter!!.symbol,
            dispatchReceiver = IrGetObjectValueImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = clazz.defaultType,
                symbol = clazz.symbol
            )
        ).markAsComposableSingleton()
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        val declarationContext = declarationContextStack.peek()
            ?: return super.visitFunctionExpression(expression)
        return if (expression.function.allowsComposableCalls)
            visitComposableFunctionExpression(expression, declarationContext)
        else
            visitNonComposableFunctionExpression(expression)
    }

    private fun startCollector(collector: CaptureCollector) {
        for (declarationContext in declarationContextStack) {
            declarationContext.pushCollector(collector)
        }
    }

    private fun stopCollector(collector: CaptureCollector) {
        for (declarationContext in declarationContextStack) {
            declarationContext.popCollector(collector)
        }
    }

    private fun wrapFunctionExpression(
        declarationContext: DeclarationContext,
        expression: IrFunctionExpression,
        collector: CaptureCollector
    ): IrExpression {
        val function = expression.function
        val argumentCount = function.valueParameters.size

        if (argumentCount > MAX_RESTART_ARGUMENT_COUNT && !context.platform.isJvm()) {
            error(
                "only $MAX_RESTART_ARGUMENT_COUNT parameters " +
                    "in @Composable lambda are supported on" +
                    "non-JVM targets (K/JS or K/Wasm or K/Native)"
            )
        }

        val useComposableLambdaN = argumentCount > MAX_RESTART_ARGUMENT_COUNT
        val useComposableFactory = collector.hasCaptures && declarationContext.composable
        val rememberComposable = rememberComposableLambdaFunction ?: composableLambdaFunction
        val requiresExplicitComposerParameter = useComposableFactory &&
            rememberComposableLambdaFunction == null
        val restartFactorySymbol =
            if (useComposableFactory)
                if (useComposableLambdaN)
                    rememberComposableLambdaNFunction ?: composableLambdaNFunction
                else rememberComposable
            else if (useComposableLambdaN)
                composableLambdaInstanceNFunction
            else composableLambdaInstanceFunction
        val irBuilder = DeclarationIrBuilder(
            context,
            symbol = declarationContext.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        // FIXME: We should remove this call once we are sure that there is nothing relying on it.
        //        `IrPluginContextImpl` is K1 specific and `getDeclaration` doesn't do anything on
        //        the JVM backend where we produce lazy declarations for unbound symbols.
        (context as? IrPluginContextImpl)?.linker?.getDeclaration(restartFactorySymbol)
        val composableLambdaExpression = irBuilder.irCall(restartFactorySymbol).apply {
            var index = 0

            // first parameter is the composer parameter if we are using the composable factory
            if (requiresExplicitComposerParameter) {
                putValueArgument(
                    index++,
                    irCurrentComposer()
                )
            }

            // key parameter
            putValueArgument(
                index++,
                irBuilder.irInt(expression.function.sourceKey())
            )

            // tracked parameter
            // If the lambda has no captures, then kotlin will turn it into a singleton instance,
            // which means that it will never change, thus does not need to be tracked.
            val shouldBeTracked = collector.captures.isNotEmpty()
            putValueArgument(index++, irBuilder.irBoolean(shouldBeTracked))

            // ComposableLambdaN requires the arity
            if (useComposableLambdaN) {
                // arity parameter
                putValueArgument(index++, irBuilder.irInt(argumentCount))
            }
            if (index >= valueArgumentsCount) {
                error(
                    "function = ${
                        function.name.asString()
                    }, count = $valueArgumentsCount, index = $index"
                )
            }

            // block parameter
            putValueArgument(index, expression.markIsTransformedLambda())
        }

        return composableLambdaExpression.markHasTransformedLambda()
    }

    private fun rememberExpression(
        functionContext: FunctionContext,
        expression: IrExpression,
        captures: List<IrValueDeclaration>
    ): IrExpression {
        // Kotlin/JS doesn't have an optimization for non-capturing lambdas
        // https://youtrack.jetbrains.com/issue/KT-49923
        val skipNonCapturingLambdas = !context.platform.isJs() && !context.platform.isWasm()

        // If the function doesn't capture, Kotlin's default optimization is sufficient
        if (captures.isEmpty() && skipNonCapturingLambdas) {
            metrics.recordLambda(
                composable = false,
                memoized = true,
                singleton = true
            )
            return expression.markAsStatic(true)
        }

        // Don't memoize if the function is annotated with DontMemoize of
        // captures any var declarations, unstable values,
        // or inlined lambdas.
        if (
            functionContext.declaration.hasAnnotation(ComposeFqNames.DontMemoize) ||
            expression.hasDontMemoizeAnnotation ||
            captures.any {
                it.isVar() ||
                    (!it.isStable() && !FeatureFlag.StrongSkipping.enabled) ||
                    it.isInlinedLambda()
            }
        ) {
            metrics.recordLambda(
                composable = false,
                memoized = false,
                singleton = false
            )
            return expression
        }

        val captureExpressions = captures.map { irGet(it) }
        metrics.recordLambda(
            composable = false,
            memoized = true,
            singleton = false
        )

        return if (!FeatureFlag.IntrinsicRemember.enabled) {
            // generate cache directly only if strong skipping is enabled without intrinsic remember
            // otherwise, generated memoization won't benefit from capturing changed values
            irCache(captureExpressions, expression)
        } else {
            irRemember(captureExpressions, expression)
        }.patchDeclarationParents(functionContext.declaration)
    }

    private fun irCache(
        captures: List<IrExpression>,
        expression: IrExpression,
    ): IrExpression {
        val invalidExpr = captures
            .map(::irChanged)
            .reduceOrNull { acc, changed -> irBooleanOr(acc, changed) }
            ?: irConst(false)

        val calculation = irLambdaExpression(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            returnType = expression.type
        ) { fn ->
            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                +irReturn(expression)
            }
        }

        val cache = irCache(
            irCurrentComposer(),
            expression.startOffset,
            expression.endOffset,
            expression.type,
            invalidExpr,
            calculation
        )

        return if (useNonSkippingGroupOptimization) {
            cache
        } else {
            // If the non-skipping group optimization is disabled then we need to wrap
            // the call to `cache` in a replaceable group.
            val fqName = currentFunctionContext?.declaration?.kotlinFqName?.asString()
            val key = fqName.hashCode() + expression.startOffset
            val cacheTmpVar = irTemporary(cache, "tmpCache")
            cacheTmpVar.wrap(
                type = expression.type,
                before = listOf(irStartReplaceGroup(irCurrentComposer(), irConst(key))),
                after = listOf(
                    irEndReplaceGroup(irCurrentComposer()),
                    irGet(cacheTmpVar)
                )
            )
        }
    }

    private fun irRemember(
        captures: List<IrExpression>,
        expression: IrExpression
    ): IrExpression {
        val directRememberFunction = // Exclude the varargs version
            rememberFunctions.singleOrNull {
                // captures + calculation arg
                it.valueParameters.size == captures.size + 1 &&
                    // Exclude the varargs version
                    it.valueParameters.firstOrNull()?.varargElementType == null
            }
        val rememberFunction = directRememberFunction
            ?: rememberFunctions.single {
                // Use the varargs version
                it.valueParameters.firstOrNull()?.varargElementType != null
            }

        val rememberFunctionSymbol = referenceSimpleFunction(rememberFunction.symbol)
        val irBuilder = DeclarationIrBuilder(
            generatorContext = context,
            symbol = currentFunctionContext!!.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        return irBuilder.irCall(
            callee = rememberFunctionSymbol,
            type = expression.type,
            origin = ComposeMemoizedLambdaOrigin
        ).apply {
            // The result type type parameter is first, followed by the argument types
            putTypeArgument(0, expression.type)
            val lambdaArgumentIndex = if (directRememberFunction != null) {
                // condition arguments are the first `arg.size` arguments
                for (i in captures.indices) {
                    putValueArgument(i, captures[i])
                }
                // The lambda is the last parameter
                captures.size
            } else {
                val parameterType = rememberFunction.valueParameters[0].type
                // Call to the vararg version
                putValueArgument(
                    0,
                    IrVarargImpl(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        type = parameterType,
                        varargElementType = context.irBuiltIns.anyType,
                        elements = captures
                    )
                )
                1
            }

            putValueArgument(
                index = lambdaArgumentIndex,
                valueArgument = irLambdaExpression(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    returnType = expression.type
                ) { fn ->
                    fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                        +irReturn(expression)
                    }
                }
            )
        }
    }

    private fun irChanged(value: IrExpression): IrExpression = irChanged(
        irCurrentComposer(),
        value,
        inferredStable = false,
        compareInstanceForFunctionTypes = false,
        compareInstanceForUnstableValues = FeatureFlag.StrongSkipping.enabled
    )

    private fun IrValueDeclaration.isVar(): Boolean =
        (this as? IrVariable)?.isVar == true

    private fun IrValueDeclaration.isStable(): Boolean =
        stabilityInferencer.stabilityOf(type).knownStable()

    private fun IrValueDeclaration.isInlinedLambda(): Boolean =
        isInlineableFunction() &&
            this is IrValueParameter &&
            (parent as? IrFunction)?.isInline == true &&
            !isNoinline

    private fun IrValueDeclaration.isInlineableFunction(): Boolean =
        type.isFunctionOrKFunction() ||
            type.isSyntheticComposableFunction() ||
            type.isSuspendFunctionOrKFunction()

    private fun <T : IrExpression> T.markAsStatic(mark: Boolean): T {
        if (mark) {
            // Mark it so the ComposableCallTransformer will insert the correct code around this
            // call
            context.irTrace.record(
                ComposeWritableSlices.IS_STATIC_FUNCTION_EXPRESSION,
                this,
                true
            )
        }
        return this
    }

    private fun <T : IrAttributeContainer> T.markAsComposableSingleton(): T {
        // Mark it so the ComposableCallTransformer can insert the correct source information
        // around this call
        context.irTrace.record(
            ComposeWritableSlices.IS_COMPOSABLE_SINGLETON,
            this,
            true
        )
        return this
    }

    private fun <T : IrAttributeContainer> T.markAsComposableSingletonClass(): T {
        // Mark it so the ComposableCallTransformer can insert the correct source information
        // around this call
        context.irTrace.record(
            ComposeWritableSlices.IS_COMPOSABLE_SINGLETON_CLASS,
            this,
            true
        )
        return this
    }

    private fun <T : IrAttributeContainer> T.markHasTransformedLambda(): T {
        // Mark so that the target annotation transformer can find the original lambda
        context.irTrace.record(
            ComposeWritableSlices.HAS_TRANSFORMED_LAMBDA,
            this,
            true
        )
        return this
    }

    private fun <T : IrAttributeContainer> T.markIsTransformedLambda(): T {
        context.irTrace.record(
            ComposeWritableSlices.IS_TRANSFORMED_LAMBDA,
            this,
            true
        )
        return this
    }

    private val IrExpression.hasDontMemoizeAnnotation: Boolean
        get() = (this as? IrFunctionExpression)?.function?.hasAnnotation(ComposeFqNames.DontMemoize)
            ?: false

    private fun IrExpression?.isNullOrStable() =
        this == null ||
            stabilityInferencer.stabilityOf(this).knownStable()
}

// This must match the highest value of FunctionXX which is current Function22
private const val MAX_RESTART_ARGUMENT_COUNT = 22

internal object ComposeMemoizedLambdaOrigin : IrStatementOrigin {
    override val debugName: String
        get() = "ComposeMemoizedLambdaOrigin"
}
