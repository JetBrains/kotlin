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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.compiler.plugins.kotlin.analysis.knownStable
import androidx.compose.compiler.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.codegen.anyTypeArgument
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
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
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrValueAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.substitute
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.types.typeUtil.isUnit

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

private fun List<DeclarationContext>.recordLocalCapture(local: IrSymbolOwner) {
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
}

private class SymbolOwnerContext(override val declaration: IrSymbolOwner) : DeclarationContext() {
    override val composable get() = false
    override val functionContext: FunctionContext? get() = null
    override val symbol get() = declaration.symbol
    override val captures: Set<IrValueDeclaration> get() = emptySet()
    override fun declareLocal(local: IrValueDeclaration?) { }
    override fun recordCapture(local: IrValueDeclaration?): Boolean { return false }
    override fun recordCapture(local: IrSymbolOwner?) { }
    override fun pushCollector(collector: CaptureCollector) { }
    override fun popCollector(collector: CaptureCollector) { }
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
    override fun declareLocal(local: IrValueDeclaration?) { }
    override fun recordCapture(local: IrValueDeclaration?): Boolean {
        val isThis = local == thisParam
        val isCtorParam = (local?.parent as? IrConstructor)?.parent === declaration
        if (local != null && collectors.isNotEmpty() && isThis) {
            for (collector in collectors) {
                collector.recordCapture(local)
            }
        }
        if (local != null && declaration.isLocal && !isThis && !isCtorParam) {
            captures.add(local)
        }
        return isThis || isCtorParam
    }
    override fun recordCapture(local: IrSymbolOwner?) { }
    override fun pushCollector(collector: CaptureCollector) {
        collectors.add(collector)
    }
    override fun popCollector(collector: CaptureCollector) {
        require(collectors.lastOrNull() == collector)
        collectors.removeAt(collectors.size - 1)
    }
}

const val COMPOSABLE_LAMBDA = "composableLambda"
const val COMPOSABLE_LAMBDA_N = "composableLambdaN"
const val COMPOSABLE_LAMBDA_INSTANCE = "composableLambdaInstance"
const val COMPOSABLE_LAMBDA_N_INSTANCE = "composableLambdaNInstance"

class ComposerLambdaMemoization(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    metrics: ModuleMetrics,
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace, metrics),
    ModuleLoweringPass {

    private val declarationContextStack = mutableListOf<DeclarationContext>()

    private val currentFunctionContext: FunctionContext? get() =
        declarationContextStack.peek()?.functionContext

    private var composableSingletonsClass: IrClass? = null
    private var currentFile: IrFile? = null

    private var inlineLambdaInfo = ComposeInlineLambdaLocator(context)

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
            }.also { ctor ->
                ctor.body = DeclarationIrBuilder(context, it.symbol).irBlockBody {
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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun irCurrentComposer(): IrExpression {
        val currentComposerSymbol = getTopLevelPropertyGetter(
            ComposeFqNames.fqNameFor("currentComposer")
        )

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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitFunction(declaration: IrFunction): IrStatement {
        val descriptor = declaration.descriptor
        val composable = declaration.allowsComposableCalls
        val canRemember = composable &&
            // Don't use remember in an inline function
            !descriptor.isInline &&
            // Don't use remember if in a composable that returns a value
            // TODO(b/150390108): Consider allowing remember in effects
            descriptor.returnType.let { it != null && it.isUnit() }

        val context = FunctionContext(declaration, composable, canRemember)
        declarationContextStack.push(context)
        val result = super.visitFunction(declaration)
        declarationContextStack.pop()
        if (declaration.isLocal) {
            declarationContextStack.recordLocalDeclaration(context)
        }
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val context = ClassContext(declaration)
        declarationContextStack.push(context)
        val result = super.visitClass(declaration)
        declarationContextStack.pop()
        if (declaration.isLocal) {
            declarationContextStack.recordLocalDeclaration(context)
        }
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

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        // Memoize the instance created by using the :: operator
        val result = super.visitFunctionReference(expression)
        val functionContext = currentFunctionContext ?: return result
        if (expression.valueArgumentsCount != 0) {
            // If this syntax is as a curry syntax in the future, don't memoize.
            // The syntax <expr>::<method>(<params>) and ::<function>(<params>) is reserved for
            // future use. This ensures we don't try to memoize this syntax without knowing
            // its meaning.

            // The most likely correct implementation is to treat the parameters exactly as the
            // receivers are treated below.
            return result
        }
        if (functionContext.canRemember) {
            // Memoize the reference for <expr>::<method>
            val dispatchReceiver = expression.dispatchReceiver
            val extensionReceiver = expression.extensionReceiver
            if ((dispatchReceiver != null || extensionReceiver != null) &&
                dispatchReceiver.isNullOrStable() &&
                extensionReceiver.isNullOrStable()
            ) {
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
                    val captures = mutableListOf<IrValueDeclaration>()

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

                    +rememberExpression(
                        functionContext,
                        IrFunctionReferenceImpl(
                            startOffset,
                            endOffset,
                            expression.type,
                            expression.symbol,
                            expression.typeArgumentsCount,
                            expression.valueArgumentsCount,
                            expression.reflectionTarget
                        ).copyAttributes(expression).apply {
                            this.dispatchReceiver = tempDispatchReceiver?.let { irGet(it) }
                            this.extensionReceiver = tempExtensionReceiver?.let { irGet(it) }
                        },
                        captures
                    )
                }
            } else if (dispatchReceiver == null) {
                return rememberExpression(functionContext, result, emptyList())
            }
        }
        return result
    }

    private fun visitNonComposableFunctionExpression(
        expression: IrFunctionExpression
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

    @ObsoleteDescriptorBasedAPI
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
        if (!functionExpression.function.descriptor.returnType.let { it == null || it.isUnit() }) {
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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
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

    @ObsoleteDescriptorBasedAPI
    private fun wrapFunctionExpression(
        declarationContext: DeclarationContext,
        expression: IrFunctionExpression,
        collector: CaptureCollector
    ): IrExpression {
        val function = expression.function
        val argumentCount = function.valueParameters.size

        val isJs = context.moduleDescriptor.platform.isJs()
        if (argumentCount > MAX_RESTART_ARGUMENT_COUNT && isJs) {
            error(
                "only $MAX_RESTART_ARGUMENT_COUNT parameters " +
                    "in @Composable lambda are supported on JS"
            )
        }

        val useComposableLambdaN = argumentCount > MAX_RESTART_ARGUMENT_COUNT
        val useComposableFactory = collector.hasCaptures && declarationContext.composable
        val restartFunctionFactory =
            if (useComposableFactory)
                if (useComposableLambdaN)
                    COMPOSABLE_LAMBDA_N
                else COMPOSABLE_LAMBDA
            else if (useComposableLambdaN)
                COMPOSABLE_LAMBDA_N_INSTANCE
            else COMPOSABLE_LAMBDA_INSTANCE
        val restartFactorySymbol =
            getTopLevelFunction(ComposeFqNames.internalFqNameFor(restartFunctionFactory))
        val irBuilder = DeclarationIrBuilder(
            context,
            symbol = declarationContext.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        (context as IrPluginContextImpl).linker.getDeclaration(restartFactorySymbol)
        val composableLambdaExpression = irBuilder.irCall(restartFactorySymbol).apply {
            var index = 0

            // first parameter is the composer parameter if we are using the composable factory
            if (useComposableFactory) {
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

        return if (!isJs) {
            composableLambdaExpression.markHasTransformedLambda()
        } else {
            /*
             * JS doesn't have ability to extend FunctionN types, therefore the lambda call must be
             * transformed into composableLambda(...)::invoke. It loses some of the optimizations
             * related to skipping updates that way, but still ensures correct handling of
             * lambdas.
             */
            val realArgumentCount = argumentCount +
                if (function.extensionReceiverParameter != null) 1 else 0

            val invokeArgumentCount = realArgumentCount +
                /*composer*/ 1 +
                changedParamCount(realArgumentCount, 0)

            val invokeSymbol = composableLambdaExpression.type.classOrNull!!
                .functions
                .single {
                    it.owner.name.asString() == "invoke" &&
                        invokeArgumentCount == it.owner.valueParameters.size
                }

            IrFunctionReferenceImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                type = expression.type,
                symbol = invokeSymbol,
                typeArgumentsCount = invokeSymbol.owner.typeParameters.size,
                valueArgumentsCount = invokeSymbol.owner.valueParameters.size
            ).also { reference ->
                reference.dispatchReceiver = composableLambdaExpression
            }
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun rememberExpression(
        functionContext: FunctionContext,
        expression: IrExpression,
        captures: List<IrValueDeclaration>
    ): IrExpression {
        // Kotlin/JS doesn't have an optimization for non-capturing lambdas
        // https://youtrack.jetbrains.com/issue/KT-49923
        val skipNonCapturingLambdas = !context.platform.isJs()

        // If the function doesn't capture, Kotlin's default optimization is sufficient
        if (captures.isEmpty() && skipNonCapturingLambdas) {
            metrics.recordLambda(
                composable = false,
                memoized = true,
                singleton = true
            )
            return expression.markAsStatic(true)
        }

        // If the function captures any unstable values or var declarations, do not memoize
        if (captures.any {
            !((it as? IrVariable)?.isVar != true && stabilityOf(it.type).knownStable())
        }
        ) {
            metrics.recordLambda(
                composable = false,
                memoized = false,
                singleton = false
            )
            return expression
        }

        // Otherwise memoize the expression based on the stable captured values
        val rememberParameterCount = captures.size + 1 // One additional parameter for the lambda
        val declaration = functionContext.declaration
        val rememberFunctions = getTopLevelFunctions(
            ComposeFqNames.fqNameFor("remember")
        ).map { it.owner }

        val directRememberFunction = // Exclude the varargs version
            rememberFunctions.singleOrNull {
                it.valueParameters.size == rememberParameterCount &&
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
            symbol = functionContext.symbol,
            startOffset = expression.startOffset,
            endOffset = expression.endOffset
        )

        metrics.recordLambda(
            composable = false,
            memoized = true,
            singleton = false
        )

        return irBuilder.irCall(
            callee = rememberFunctionSymbol,
            type = expression.type
        ).apply {
            // The result type type parameter is first, followed by the argument types
            putTypeArgument(0, expression.type)
            val lambdaArgumentIndex = if (directRememberFunction != null) {
                // condition arguments are the first `arg.size` arguments
                for (i in captures.indices) {
                    putValueArgument(i, irBuilder.irGet(captures[i]))
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
                        elements = captures.map {
                            irBuilder.irGet(it)
                        }
                    )
                )
                1
            }

            val substitutedLambdaType = rememberFunction.valueParameters.last().type.substitute(
                rememberFunction.typeParameters,
                (0 until typeArgumentsCount).map {
                    getTypeArgument(it) as IrType
                }
            )
            putValueArgument(
                lambdaArgumentIndex,
                irBuilder.irLambdaExpression(
                    descriptor = irBuilder.createFunctionDescriptor(
                        substitutedLambdaType
                    ),
                    type = substitutedLambdaType,
                    body = {
                        +irReturn(expression)
                    }
                )
            )
        }.patchDeclarationParents(declaration).markAsSynthetic(mark = true)
    }

    private fun <T : IrFunctionAccessExpression> T.markAsSynthetic(mark: Boolean): T {
        if (mark) {
            // Mark it so the ComposableCallTransformer will insert the correct code around this
            // call
            context.irTrace.record(
                ComposeWritableSlices.IS_SYNTHETIC_COMPOSABLE_CALL,
                this,
                true
            )
        }
        return this
    }

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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrExpression?.isNullOrStable() = this == null || stabilityOf(this).knownStable()
}

// This must match the highest value of FunctionXX which is current Function22
private const val MAX_RESTART_ARGUMENT_COUNT = 22
