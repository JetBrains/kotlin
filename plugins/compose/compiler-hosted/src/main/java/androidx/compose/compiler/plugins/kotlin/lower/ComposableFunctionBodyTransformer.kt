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

import androidx.compose.compiler.plugins.kotlin.*
import androidx.compose.compiler.plugins.kotlin.analysis.*
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.ADAPTER_FOR_CALLABLE_REFERENCE
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.min

/**
 * An enum of the different "states" a parameter of a composable function can have relating to
 * comparison propagation. Each state is represented by two bits in the `$changed` bitmask.
 */
enum class ParamState(val bits: Int) {
    /**
     * Indicates that nothing is certain about the current state of the parameter. It could be
     * different than it was during the last execution, or it could be the same, but it is not
     * known so the current function looking at it must call equals on it in order to find out.
     * This is the only state that can cause the function to spend slot table space in order to
     * look at it.
     */
    Uncertain(0b000),

    /**
     * This indicates that the value is known to be the same since the last time the function was
     * executed. There is no need to store the value in the slot table in this case because the
     * calling function will *always* know whether the value was the same or different as it was
     * in the previous execution.
     */
    Same(0b001),

    /**
     * This indicates that the value is known to be different since the last time the function
     * was executed. There is no need to store the value in the slot table in this case because
     * the calling function will *always* know whether the value was the same or different as it
     * was in the previous execution.
     */
    Different(0b010),

    /**
     * This indicates that the value is known to *never change* for the duration of the running
     * program.
     */
    Static(0b011),

    @Suppress("unused")
    Unknown(0b100),
    Mask(0b111);

    fun bitsForSlot(slot: Int): Int = bitsForSlot(bits, slot)
}

const val BITS_PER_INT = 31
const val SLOTS_PER_INT = 10
const val BITS_PER_SLOT = 3

fun bitsForSlot(bits: Int, slot: Int): Int {
    val realSlot = slot.rem(SLOTS_PER_INT)
    return bits shl (realSlot * BITS_PER_SLOT + 1)
}

fun defaultsParamIndex(index: Int): Int = index / BITS_PER_INT
fun defaultsBitIndex(index: Int): Int = index.rem(BITS_PER_INT)

/**
 * The number of implicit ('this') parameters the function has.
 */
val IrFunction.thisParamCount
    get() = parameters.count {
        it.kind == IrParameterKind.DispatchReceiver ||
                it.kind == IrParameterKind.Context ||
                it.kind == IrParameterKind.ExtensionReceiver
    }

/**
 * Calculates the number of 'changed' params needed based on the function's parameters.
 *
 * @param realValueParams The number of params defined by the user, those that are not implicit
 * (no extension or context receivers) or synthetic (no %composer, %changed or %defaults).
 * @param thisParams The number of implicit params, i.e. [IrFunction.thisParamCount]
 */
fun changedParamCount(realValueParams: Int, thisParams: Int): Int {
    val totalParams = realValueParams + thisParams
    if (totalParams == 0) return 1 // There is always at least 1 changed param
    return ceil(
        totalParams.toDouble() / SLOTS_PER_INT.toDouble()
    ).toInt()
}

/**
 * Calculates the number of 'changed' params needed based on the function's total amount of
 * parameters.
 *
 * @param totalParamsIncludingThisParams The total number of parameter including implicit and
 * synthetic ones.
 */
fun changedParamCountFromTotal(totalParamsIncludingThisParams: Int): Int {
    var realParams = totalParamsIncludingThisParams
    realParams-- // composer param
    realParams-- // first changed param (always present)
    var changedParams = 0
    do {
        realParams -= SLOTS_PER_INT
        changedParams++
    } while (realParams > 0)
    return changedParams
}

/**
 * Calculates the number of 'defaults' params needed based on the function's parameters.
 *
 * @param valueParams The numbers of params, usually the size of [IrFunction.parameters] with [IrParameterKind.Regular].
 * Which includes context receivers params, but not extension param nor synthetic params.
 */
fun defaultParamCount(valueParams: Int): Int {
    return ceil(
        valueParams.toDouble() / BITS_PER_INT.toDouble()
    ).toInt()
}

fun composeSyntheticParamCount(
    realValueParams: Int,
    thisParams: Int = 0,
): Int {
    return 1 + // composer param
            changedParamCount(realValueParams, thisParams)
}

@JvmDefaultWithCompatibility
interface IrChangedBitMaskValue {
    val used: Boolean
    val declarations: List<IrValueDeclaration>
    fun irLowBit(): IrExpression
    fun irIsolateBitsAtSlot(slot: Int, includeStableBit: Boolean): IrExpression
    fun irSlotAnd(slot: Int, bits: Int): IrExpression
    fun irHasDifferences(usedParams: BooleanArray): IrExpression
    fun irRestartFlags(): IrExpression
    fun irCopyToTemporary(
        nameHint: String? = null,
        isVar: Boolean = false,
        exactName: Boolean = false,
    ): IrChangedBitMaskVariable

    fun putAsValueArgumentInWithLowBit(
        fn: IrFunctionAccessExpression,
        startIndex: Int,
        lowBit: Boolean,
    )

    fun irShiftBits(fromSlot: Int, toSlot: Int): IrExpression
    fun irStableBitAtSlot(slot: Int): IrExpression
}

interface IrDefaultBitMaskValue {
    fun irIsolateBitAtIndex(index: Int): IrExpression
    fun irHasAnyProvidedAndUnstable(unstable: BooleanArray): IrExpression
    fun putAsValueArgumentIn(fn: IrFunctionAccessExpression, startIndex: Int)
}

@JvmDefaultWithCompatibility
interface IrChangedBitMaskVariable : IrChangedBitMaskValue {
    fun asStatements(): List<IrStatement>
    fun irOrSetBitsAtSlot(slot: Int, value: IrExpression): IrExpression
    fun irSetSlotUncertain(slot: Int): IrExpression
}

/**
 * This IR Transform is responsible for the main transformations of the body of a composable
 * function.
 *
 * 1. Control-Flow Group Generation
 * 2. Default arguments
 * 3. Composable Function Skipping
 * 4. Comparison Propagation
 * 5. Recomposability
 * 6. Source location information (when enabled)
 *
 * Control-Flow Group Generation
 * =============================
 *
 * This transform will insert groups inside of the bodies of Composable functions
 * depending on the control-flow structures that exist inside of them.
 *
 * There are 3 types of groups in Compose:
 *
 * 1. Replace Groups
 * 2. Movable Groups
 * 3. Restart Groups
 *
 * Generally speaking, every composable function *must* emit a single group when it executes.
 * Every group can have any number of children groups. Additionally, we analyze each executable
 * block and apply the following rules:
 *
 * 1. If a block executes exactly 1 time always, no groups are needed
 * 2. If a set of blocks are such that exactly one of them is executed exactly once (for example,
 * the result blocks of a when clause), then we insert a replace group around each block.
 * 3. A movable group is only needed if the immediate composable call in the group has a Pivotal
 * property.
 *
 * Default Arguments
 * =================
 *
 * Composable functions need to have the default expressions executed inside of the group of the
 * function. In order to accomplish this, composable functions handle default arguments
 * themselves, instead of using the default handling of kotlin. This is also a win because we can
 * handle the default arguments without generating an additional function since we do not need to
 * worry about callers from java. Generally speaking though, compose handles default arguments
 * similarly to kotlin in that we generate a $default bitmask parameter which maps each parameter
 * index to a bit on the int. A value of "1" for a given parameter index indicated that that
 * value was *not* provided at the callsite, and the default expression should be used instead.
 *
 *     @Composable fun A(x: Int = 0) {
 *       f(x)
 *     }
 *
 * gets transformed into
 *
 *     @Composable fun A(x: Int, $default: Int) {
 *       val x = if ($default and 0b1 != 0) 0 else x
 *       f(x)
 *     }
 *
 * Note: This transform requires [ComposerParamTransformer] to also be run in order to work
 * properly.
 *
 * Composable Function Skipping
 * ============================
 *
 * Composable functions can "skip" their execution if certain conditions are met. This is done by
 * appealing to the composer and storing previous values of functions and determining if we can
 * skip based on whether or not they have changed.
 *
 *     @Composable fun A(x: Int) {
 *       f(x)
 *     }
 *
 * gets transformed into
 *
 *     @Composable fun A(x: Int, $composer: Composer<*>, $changed: Int) {
 *       var $dirty = $changed
 *       if ($changed and 0b0110 == 0) {
 *         $dirty = $dirty or if ($composer.changed(x)) 0b0010 else 0b0100
 *       }
 *      if (%dirty and 0b1011 != 0b1010 || !$composer.skipping) {
 *        f(x)
 *      } else {
 *        $composer.skipToGroupEnd()
 *      }
 *     }
 *
 * Note that this makes use of bitmasks for the $changed and $dirty values. These bitmasks work
 * in a different bit-space than the $default bitmask because three bits are needed to hold the
 * six different possible states of each parameter. Additionally, the lowest bit of the bitmask
 * is a special bit which forces execution of the function.
 *
 * This means that for the ith parameter of a composable function, the bit range of i*3 + 1 to
 * i*3 + 3 are used to store the state of the parameter.
 *
 * The states are outlines by the [ParamState] class.
 *
 * Comparison Propagation
 * ======================
 *
 * Because we detect changes in parameters of composable functions and have that data available
 * in the body of a composable function, if we pass values to another composable function, it
 * makes sense for us to pass on whatever information about that value we can determine at the
 * time. This type of propagation of information through composable functions is called
 * Comparison Propagation.
 *
 * Essentially, this comes down to us passing in useful values into the `$changed` parameter of
 * composable functions.
 *
 * When a composable function executes, we have the current known states of all of the function's
 * parameters in the $dirty variable. We can take bits off of this variable and pass them into a
 * composable function in order to tell that function what we know.
 *
 *     @Composable fun A(x: Int) {
 *       B(x, 123)
 *     }
 *
 * gets transformed into
 *
 *     @Composable fun A(x: Int, $composer: Composer<*>, $changed: Int) {
 *       var $dirty = ...
 *       // ...
 *       B(
 *           x,
 *           123,
 *           $composer,
 *           (0b110 and $dirty) or   // 1st param has same state that our 1st param does
 *           0b11000                 // 2nd parameter is "static"
 *       )
 *     }
 *
 * Recomposability
 * ===============
 *
 * Restartable composable functions get wrapped with "restart groups". Restart groups are like
 * other groups except the end call is more complicated, as it returns a null value if and
 * only if a subscription to that scope could not have occurred. If the value returned is
 * non-null, we generate a lambda that teaches the runtime how to "restart" that group. At a high
 * level, this transform comes down to:
 *
 *     @Composable fun A(x: Int) {
 *       f(x)
 *     }
 *
 * getting transformed into
 *
 *     @Composable fun A(x: Int, $composer: Composer<*>, $changed: Int) {
 *       $composer.startRestartGroup()
 *       // ...
 *       f(x)
 *       $composer.endRestartGroup()?.updateScope { next -> A(x, next, $changed or 0b1) }
 *     }
 *
 * Source information
 * ==================
 * To enable Android Studio and similar tools to inspect a composition, source information is
 * optionally generated into the source to indicate where call occur in a block. The first group
 * of every function is also marked to correspond to indicate that the group corresponds to a call
 * and the source location of the caller can be determined from the containing group.
 */
class ComposableFunctionBodyTransformer(
    context: IrPluginContext,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    private val collectSourceInformation: Boolean,
    private val traceMarkersEnabled: Boolean,
    featureFlags: FeatureFlags,
) :
    AbstractComposeLowering(context, metrics, stabilityInferencer, featureFlags),
    FileLoweringPass,
    ModuleLoweringPass {

    private var inlineLambdaInfo = ComposeInlineLambdaLocator(context)

    override fun lower(irModule: IrModuleFragment) {
        inlineLambdaInfo.scan(irModule)
        irModule.transformChildrenVoid(this)
        applySourceFixups()
        irModule.patchDeclarationParents()
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
        applySourceFixups()
    }

    private val skipToGroupEndFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "skipToGroupEnd" && it.parameters.size == 1
            }
    }

    // todo is this correct to be unused?
    private val skipCurrentGroupFunction by guardedLazy {
        composerIrClass
            .functions
            .first {
                it.name.identifier == "skipCurrentGroup" && it.parameters.size == 1
            }
    }

    private val startDefaultsFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "startDefaults" && it.parameters.size == 1
            }
    }

    private val endDefaultsFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "endDefaults" && it.parameters.size == 1
            }
    }

    private val startMovableFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "startMovableGroup" && it.parameters.size == 3
            }
    }

    private val endMovableFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "endMovableGroup" && it.parameters.size == 1
            }
    }

    private val startRestartGroupFunction by guardedLazy {
        composerIrClass
            .functions
            .first {
                it.name == ComposeNames.StartRestartGroup && it.parameters.size == 2
            }
    }

    private val currentMarkerProperty: IrProperty? by guardedLazy {
        composerIrClass.properties
            .firstOrNull {
                it.name == ComposeNames.CurrentMarker
            }
    }

    private val endToMarkerFunction: IrSimpleFunction? by guardedLazy {
        composerIrClass
            .functions
            .firstOrNull {
                it.name == ComposeNames.EndToMarker && it.parameters.size == 2
            }
    }

    private val rollbackGroupMarkerEnabled
        get() =
            currentMarkerProperty != null && endToMarkerFunction != null

    private val endRestartGroupFunction by guardedLazy {
        composerIrClass
            .functions
            .first {
                it.name == ComposeNames.EndRestartGroup && it.parameters.size == 1
            }
    }

    private val shouldExecuteFunction by guardedLazy {
        if (FeatureFlag.PausableComposition.enabled)
            composerIrClass
                .functions
                .firstOrNull {
                    it.name == ComposeNames.ShouldExecute &&
                            it.parameters.size == 3 &&
                            it.parameters[1].type.isBoolean() &&
                            it.parameters[2].type.isInt()
                }
        else null
    }

    private val sourceInformationFunction by guardedLazy {
        getTopLevelFunction(ComposeCallableIds.sourceInformation).owner
    }

    private val sourceInformationMarkerStartFunction by guardedLazy {
        getTopLevelFunction(ComposeCallableIds.sourceInformationMarkerStart).owner
    }

    private val updateChangedFlagsFunction: IrSimpleFunction? by guardedLazy {
        getTopLevelFunctionOrNull(
            ComposeCallableIds.updateChangedFlags
        )?.let {
            val owner = it.owner
            if (owner.parameters.size == 1) owner else null
        }
    }

    private val isTraceInProgressFunction by guardedLazy {
        getTopLevelFunctions(ComposeCallableIds.isTraceInProgress).singleOrNull {
            it.owner.parameters.isEmpty()
        }?.owner
    }

    private val traceEventStartFunction by guardedLazy {
        getTopLevelFunctions(ComposeCallableIds.traceEventStart).singleOrNull {
            it.owner.parameters.map { p -> p.type } == listOf(
                context.irBuiltIns.intType,
                context.irBuiltIns.intType,
                context.irBuiltIns.intType,
                context.irBuiltIns.stringType
            )
        }?.owner
    }

    private val traceEventEndFunction by guardedLazy {
        getTopLevelFunctions(ComposeCallableIds.traceEventEnd).singleOrNull {
            it.owner.parameters.isEmpty()
        }?.owner
    }

    private val traceEventMarkersEnabled
        get() =
            traceMarkersEnabled && traceEventEndFunction != null

    private val sourceInformationMarkerEndFunction by guardedLazy {
        getTopLevelFunction(ComposeCallableIds.sourceInformationMarkerEnd).owner
    }

    private val rememberComposableLambdaFunction by guardedLazy {
        getTopLevelFunctions(ComposeCallableIds.rememberComposableLambda).singleOrNull()
    }

    private val useNonSkippingGroupOptimization by guardedLazy {
        // Uses `rememberComposableLambda` as a indication that the runtime supports
        // generating remember after call as it was added at the same time as the slot table was
        // modified to support remember after call.
        FeatureFlag.OptimizeNonSkippingGroups.enabled && rememberComposableLambdaFunction != null
    }

    private val IrType.arguments: List<IrTypeArgument>
        get() = (this as? IrSimpleType)?.arguments.orEmpty()

    private val updateScopeFunction by guardedLazy {
        endRestartGroupFunction.returnType
            .classOrNull
            ?.owner
            ?.functions
            ?.singleOrNull {
                it.name == ComposeNames.UpdateScope &&
                        it.parameters.size == 2 &&
                        it.parameters[1].type.arguments.size == 3
            }
            ?: error("new updateScope not found in result type of endRestartGroup")
    }

    private val isSkippingFunction by guardedLazy {
        composerIrClass.properties
            .first {
                it.name.asString() == "skipping"
            }
    }

    private val defaultsInvalidFunction by guardedLazy {
        composerIrClass
            .properties
            .first {
                it.name.asString() == "defaultsInvalid"
            }
    }

    private val joinKeyFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name == ComposeNames.JoinKey && it.parameters.size == 3
            }
    }

    private var currentScope: Scope = Scope.RootScope()

    private fun printScopeStack(): String {
        return buildString {
            currentScope.forEach {
                appendLine(it.name)
            }
        }
    }

    private val isInComposableScope: Boolean
        get() = currentScope.isInComposable

    private val currentFunctionScope
        get() = currentScope.functionScope
            ?: error("Expected a FunctionScope but none exist. \n${printScopeStack()}")

    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.isComposableSingletonClass()) {
            return declaration
        }
        return inScope(Scope.ClassScope(declaration.name)) {
            super.visitDeclaration(declaration)
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val scope = Scope.FunctionScope(declaration, this)
        return inScope(scope) {
            visitFunctionInScope(declaration)
        }.also {
            if (scope.isInlinedLambda && !scope.isComposable && scope.hasComposableCalls) {
                encounteredCapturedComposableCall()
            }
            metrics.recordFunction(scope.metrics)
            declaration.functionMetrics = scope.metrics
        }
    }

    private fun visitFunctionInScope(declaration: IrFunction): IrStatement {
        val scope = currentFunctionScope
        // if the function isn't composable, there's nothing to do
        if (!scope.isComposable) return super.visitFunction(declaration)
        if (declaration.isDefaultParamStub) {
            // don't transform the body of the stub normally
            return visitComposableFunctionStub(declaration)
        }
        if (declaration.origin == ADAPTER_FOR_CALLABLE_REFERENCE) {
            return visitComposableReferenceAdapter(declaration, scope)
        }

        val restartable = declaration.shouldBeRestartable() && !inlineLambdaInfo.isInlineLambda(declaration)
        val isLambda = declaration.isLambda()

        val isTracked = declaration.returnType.isUnit()

        if (declaration.body == null) return declaration

        val changedParam = scope.changedParameter!!
        val defaultParam = scope.defaultParameter

        // restartable functions get extra logic and different types of groups from
        // non-restartable functions, and lambdas get no groups at all.
        return when {
            isLambda && isTracked -> visitComposableLambda(
                declaration,
                scope,
                changedParam
            )
            restartable && isTracked -> visitRestartableComposableFunction(
                declaration,
                scope,
                changedParam,
                defaultParam
            )
            else -> visitNonRestartableComposableFunction(
                declaration,
                scope,
                changedParam,
                defaultParam
            )
        }.also { function ->
            val assignableParams = function.parameters.filter { it.isAssignable }.toSet()
            val defaultArgs = assignableParams // only default args and composer are marked as `isAssignable`

            if (assignableParams.isNotEmpty()) {
                function.transform(
                    object : IrElementTransformerVoid() {
                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            if (expression.symbol.owner !in defaultArgs) {
                                return super.visitGetValue(expression)
                            }
                            val defaultParameterType = expression.type.defaultParameterType()
                            if (defaultParameterType != expression.type) {
                                return IrTypeOperatorCallImpl(
                                    expression.startOffset,
                                    expression.endOffset,
                                    expression.type,
                                    IrTypeOperator.IMPLICIT_CAST,
                                    expression.type,
                                    IrGetValueImpl(
                                        expression.startOffset,
                                        expression.endOffset,
                                        defaultParameterType,
                                        expression.symbol,
                                        expression.origin
                                    )
                                )
                            }
                            return super.visitGetValue(expression)
                        }
                    }, null
                )
            }
        }
    }

    // At a high level, without useNonSkippingGroupOptimization, a non-restartable composable
    // function
    // 1. gets a replace group placed around the body
    // 2. never calls `$composer.changed(...)` with its parameters
    // 3. can have default parameters, so needs to add the defaults preamble if defaults present
    // 4. proper groups around control flow structures in the body
    // If supported by the runtime and useNonSkippingGroupOptimization is enabled then the
    // replace group is not necessary so the above list is changed to,
    // 1. never calls `$composer.changed(...)` with its parameters
    // 2. can have default parameters, so needs to add the defaults preamble if defaults present
    // 3. never elides groups around control flow structures in the body
    // If the function has `ExplicitGroupsComposable` annotation, groups or markers should be added.
    @OptIn(IrImplementationDetail::class, IDEAPluginsCompatibilityAPI::class)
    private fun visitNonRestartableComposableFunction(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
    ): IrFunction {
        val body = declaration.body!!

        val hasExplicitGroups = declaration.hasExplicitGroups
        val isReadOnly = declaration.hasReadOnlyAnnotation || declaration.isComposableDelegatedAccessor()

        // An outer group is required if we are a lambda or dynamic method or the runtime doesn't
        // support remember after call. A outer group is explicitly elided by readonly and has
        // explicit groups.
        var outerGroupRequired =
            (!isReadOnly && !hasExplicitGroups && !useNonSkippingGroupOptimization) ||
                    declaration.isLambda() ||
                    declaration.isOverridableOrOverrides ||
                    declaration.isComposableReferenceAdapter

        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        scope.dirty = changedParam
        scope.outerGroupRequired = outerGroupRequired

        val defaultScope = transformDefaults(scope)

        var (transformed, returnVar) = body.asBodyAndResultVar()

        val emitTraceMarkers = traceEventMarkersEnabled &&
                !scope.function.isInline &&
                !declaration.isComposableReferenceAdapter

        transformed = transformed.apply {
            transformChildrenVoid()
        }

        // If we get an early return from this function then the function itself acts like
        // an if statement and the outer group is required if the functions is not readonly or has
        // explicit groups.
        if (!isReadOnly && !hasExplicitGroups && scope.hasAnyEarlyReturn) outerGroupRequired = true

        buildPreambleStatementsAndReturnIfSkippingPossible(
            body,
            skipPreamble,
            bodyPreamble,
            false,
            scope,
            changedParam,
            changedParam,
            defaultParam,
            defaultScope,
        )

        // NOTE: It's important to do this _after_ the above call since it can change the
        // value of `dirty.used`.
        if (emitTraceMarkers) {
            transformed.wrapWithTraceEvents(irFunctionSourceKey(), scope)
        }

        if (outerGroupRequired) {
            scope.realizeGroup {
                irComposite(
                    statements = listOfNotNull(
                        if (emitTraceMarkers) irTraceEventEnd() else null,
                        irEndReplaceGroup(scope = scope)
                    )
                )
            }
        } else if (useNonSkippingGroupOptimization) {
            scope.realizeAllDirectChildren()
            scope.realizeCoalescableGroup()
        }

        declaration.body = context.irFactory.createBlockBody(body.startOffset, body.endOffset).apply {
            this.statements.addAll(
                listOfNotNull(
                    when {
                        outerGroupRequired ->
                            irStartReplaceGroup(
                                body,
                                scope,
                                irFunctionSourceKey()
                            )
                        collectSourceInformation ->
                            irSourceInformationMarkerStart(
                                body,
                                scope,
                                irFunctionSourceKey()
                            )
                        else -> null
                    },
                    *scope.markerPreamble.statements.toTypedArray(),
                    *bodyPreamble.statements.toTypedArray(),
                    *transformed.statements.toTypedArray(),
                    when {
                        outerGroupRequired -> irEndReplaceGroup(scope = scope)
                        collectSourceInformation ->
                            irSourceInformationMarkerEnd(body, scope)
                        else -> null
                    },
                    returnVar?.let { irReturnVar(declaration.symbol, it) }
                )
            )
        }

        if (!outerGroupRequired) {
            scope.realizeEndCalls {
                irComposite(
                    statements = listOfNotNull(
                        if (emitTraceMarkers) irTraceEventEnd() else null,
                        if (collectSourceInformation)
                            irSourceInformationMarkerEnd(body, scope)
                        else null
                    )
                )
            }
        }

        scope.metrics.recordFunction(
            composable = true,
            restartable = false,
            skippable = false,
            isLambda = declaration.isLambda(),
            inline = declaration.isInline,
            hasDefaults = false,
            readonly = isReadOnly,
        )

        scope.metrics.recordGroup()
        return declaration
    }

    // Composable lambdas are always wrapped with a ComposableLambda class, which has its own
    // group in the invoke call. As a result, composable lambdas:
    // 1. receive no group at the root of their body
    // 2. cannot have default parameters, so have no default handling
    // 3. they cannot be skipped since we do not know their capture scope, so no skipping logic
    // 4. proper groups around control flow structures in the body
    @OptIn(IrImplementationDetail::class, IDEAPluginsCompatibilityAPI::class)
    private fun visitComposableLambda(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
    ): IrFunction {
        // no group, since composableLambda should already create one
        // no default logic
        val body = declaration.body!!
        val sourceInformationPreamble = mutableStatementContainer()
        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()
        val bodyEpilogue = mutableStatementContainer()

        val isInlineLambda = scope.isInlinedLambda

        if (collectSourceInformation && !isInlineLambda) {
            sourceInformationPreamble.statements.add(irSourceInformation(scope))
        }

        // we start off assuming that we *can* skip execution of the function
        var canSkipExecution = declaration.returnType.isUnit() &&
                !isInlineLambda &&
                scope.allTrackedParams.none { stabilityInferencer.stabilityOf(it.type).knownUnstable() }

        // if the function can never skip, or there are no parameters to test, then we
        // don't need to have the dirty parameter locally since it will never be different from
        // the passed in `changed` parameter.
        val dirty = if (canSkipExecution && scope.allTrackedParams.isNotEmpty())
        // NOTE(lmr): Technically, dirty is a mutable variable, but we don't want to mark it
        // as one since that will cause a `Ref<Int>` to get created if it is captured. Since
        // we know we will never be mutating this variable _after_ it gets captured, we can
        // safely mark this as `isVar = false`.
            changedParam.irCopyToTemporary(
                // LLVM validation doesn't allow us to have val here.
                isVar = !context.platform.isJvm() && !context.platform.isJs(),
                nameHint = $$"$dirty",
                exactName = true
            )
        else
            changedParam

        scope.dirty = dirty

        val (nonReturningBody, returnVar) = body.asBodyAndResultVar(declaration)

        val emitTraceMarkers = traceEventMarkersEnabled && !scope.isInlinedLambda

        // we must transform the body first, since that will allow us to see whether or not we
        // are using the dispatchReceiverParameter or the extensionReceiverParameter
        val transformed = nonReturningBody.apply {
            transformChildrenVoid()
        }.let {
            // Ensure that all group children of composable inline lambda are realized, since the inline
            // lambda doesn't require a group on its own.
            if (scope.isInlinedLambda && scope.isComposable) {
                scope.realizeAllDirectChildren()
            }

            if (isInlineLambda) {
                it.asSourceOrEarlyExitGroup(scope)
            } else it
        }

        canSkipExecution = buildPreambleStatementsAndReturnIfSkippingPossible(
            body,
            skipPreamble,
            bodyPreamble,
            canSkipExecution,
            scope,
            dirty,
            changedParam,
            null,
            Scope.ParametersScope(),
        )

        // NOTE: It's important to do this _after_ the above call since it can change the
        // value of `dirty.used`.
        if (emitTraceMarkers) {
            transformed.wrapWithTraceEvents(irFunctionSourceKey(), scope)
        }

        val dirtyForSkipping = if (dirty.used && dirty is IrChangedBitMaskVariable) {
            skipPreamble.statements.addAll(0, dirty.asStatements())
            dirty
        } else changedParam

        if (emitTraceMarkers) {
            scope.realizeEndCalls {
                irTraceEventEnd()!!
            }
        }

        scope.applyIntrinsicRememberFixups { isMemoizedLambda, args, metas ->
            // replace dirty with changed param in meta used for inference, as we are not
            // populating dirty
            if (!canSkipExecution) {
                metas.fastForEach {
                    if (it.paramRef?.maskParam == dirty) {
                        it.paramRef?.maskParam = changedParam
                    }
                }
            }
            irIntrinsicRememberInvalid(isMemoizedLambda, args, metas, ::irIntrinsicChanged)
        }

        if (canSkipExecution) {
            // We CANNOT skip if any of the following conditions are met
            // 1. if any of the stable parameters have *differences* from last execution.
            // 2. if the composer.skipping call returns false
            // 3. function is inline
            val shouldExecute = irShouldExecute(
                dirtyForSkipping.irHasDifferences(scope.usedParams),
                dirtyForSkipping.irRestartFlags(),
            )
            val transformedBody = irIfThenElse(
                condition = shouldExecute,
                thenPart = irBlock(
                    type = context.irBuiltIns.unitType,
                    statements = transformed.statements
                ),
                // Use end offsets so that stepping out of the composable function
                // does not step back to the start line for the function.
                elsePart = irSkipToGroupEnd(),
            )
            scope.realizeCoalescableGroup()
            declaration.body = context.irFactory.createBlockBody(body.startOffset, body.endOffset).apply {
                this.statements.addAll(
                    listOfNotNull(
                        *sourceInformationPreamble.statements.toTypedArray(),
                        *scope.markerPreamble.statements.toTypedArray(),
                        *skipPreamble.statements.toTypedArray(),
                        *bodyPreamble.statements.toTypedArray(),
                        transformedBody,
                        returnVar?.let { irReturnVar(declaration.symbol, it) }
                    )
                )
            }
        } else {
            scope.realizeCoalescableGroup()
            declaration.body = context.irFactory.createBlockBody(body.startOffset, body.endOffset).apply {
                this.statements.addAll(
                    listOfNotNull(
                        *scope.markerPreamble.statements.toTypedArray(),
                        *sourceInformationPreamble.statements.toTypedArray(),
                        *skipPreamble.statements.toTypedArray(),
                        *bodyPreamble.statements.toTypedArray(),
                        transformed,
                        *bodyEpilogue.statements.toTypedArray(),
                        returnVar?.let { irReturnVar(declaration.symbol, it) }
                    )
                )
            }
        }
        scope.metrics.recordFunction(
            composable = true,
            restartable = true,
            skippable = canSkipExecution,
            isLambda = true,
            inline = false,
            hasDefaults = false,
            readonly = false,
        )
        // composable lambdas all have a root group, but we don't generate them as the source
        // code itself has the start/end call.
        scope.metrics.recordGroup()

        return declaration
    }

    // Most composable function declarations will be restartable. At a high level, this means
    // that for this function we:
    // 1. generate a startRestartGroup and endRestartGroup call around its body
    // 2. generate an updateScope lambda and call
    // 3. generate handling of default parameters if necessary
    // 4. generate skipping logic based on parameters passed into the function
    // 5. generate groups around control flow structures in the body
    @OptIn(IrImplementationDetail::class, IDEAPluginsCompatibilityAPI::class)
    private fun visitRestartableComposableFunction(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
    ): IrFunction {
        val body = declaration.body!!
        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        // NOTE(lmr): Technically, dirty is a mutable variable, but we don't want to mark it
        // as one since that will cause a `Ref<Int>` to get created if it is captured. Since
        // we know we will never be mutating this variable _after_ it gets captured, we can
        // safely mark this as `isVar = false`.
        val dirty = if (scope.allTrackedParams.isNotEmpty())
            changedParam.irCopyToTemporary(
                // LLVM validation doesn't allow us to have val here.
                isVar = !context.platform.isJvm() && !context.platform.isJs(),
                nameHint = $$"$dirty",
                exactName = true
            )
        else
            changedParam

        scope.dirty = dirty

        val (nonReturningBody, returnVar) = body.asBodyAndResultVar()

        val end = {
            irEndRestartGroupAndUpdateScope(
                scope,
                changedParam,
                defaultParam,
                scope.realValueParamCount
            )
        }

        val endWithTraceEventEnd = {
            irComposite(
                statements = listOfNotNull(
                    if (traceEventMarkersEnabled) irTraceEventEnd() else null,
                    end()
                )
            )
        }

        val defaultScope = transformDefaults(scope)

        // we must transform the body first, since that will allow us to see whether or not we
        // are using the dispatchReceiverParameter or the extensionReceiverParameter
        val transformed = nonReturningBody.apply {
            transformChildrenVoid()
        }

        val canSkipExecution = buildPreambleStatementsAndReturnIfSkippingPossible(
            body,
            skipPreamble,
            bodyPreamble,
            // we start off assuming that we *can* skip execution of the function
            !declaration.hasNonSkippableAnnotation,
            scope,
            dirty,
            changedParam,
            defaultParam,
            defaultScope,
        )

        // NOTE: It's important to do this _after_ the above call since it can change the
        // value of `dirty.used`.
        if (traceEventMarkersEnabled) {
            transformed.wrapWithTraceEvents(irFunctionSourceKey(), scope)
        }

        // if it has non-optional unstable params, the function can never skip, so we always
        // execute the body. Otherwise, we wrap the body in an if and only skip when certain
        // conditions are met.
        val dirtyForSkipping = if (dirty.used && dirty is IrChangedBitMaskVariable) {
            skipPreamble.statements.addAll(0, dirty.asStatements())
            dirty
        } else changedParam

        scope.applyIntrinsicRememberFixups { isMemoizedLambda, args, metas ->
            // replace dirty with changed param in meta used for inference, as we are not
            // populating dirty
            if (!canSkipExecution) {
                metas.fastForEach {
                    if (it.paramRef?.maskParam == dirty) {
                        it.paramRef?.maskParam = changedParam
                    }
                }
            }
            irIntrinsicRememberInvalid(isMemoizedLambda, args, metas, ::irIntrinsicChanged)
        }

        val transformedBody = if (canSkipExecution) {
            // We CANNOT skip if any of the following conditions are met
            // 1. if any of the stable parameters have *differences* from last execution.
            // 2. if the composer.skipping call returns false
            // 3. if any of the provided parameters to the function were unstable

            // (3) is only necessary to check if we actually have unstable params, so we only
            // generate that check if we need to.
            var shouldExecute = irShouldExecute(
                dirtyForSkipping.irHasDifferences(scope.usedParams),
                dirtyForSkipping.irRestartFlags(),
            )

            // boolean array mapped to parameters. true indicates that the type is unstable
            // NOTE: the unstable mask is indexed by valueParameter index, which is different
            // than the slotIndex but that is OKAY because we only care about defaults, which
            // also use the value parameter index.
            val realParams = declaration.namedParameters.take(scope.realValueParamCount)

            val unstableMask = realParams.map {
                stabilityInferencer.stabilityOf((it.varargElementType ?: it.type)).knownUnstable()
            }.toBooleanArray()

            val hasAnyUnstableParams = unstableMask.any { it }

            // If we aren't in strong skipping mode and
            // if there are unstable params, then we fence the whole expression with a check to
            // see if any of the unstable params were the ones that were provided to the
            // function. If they were, then we short-circuit and always execute
            if (
                !FeatureFlag.StrongSkipping.enabled &&
                hasAnyUnstableParams && defaultParam != null
            ) {
                shouldExecute = irOrOr(
                    defaultParam.irHasAnyProvidedAndUnstable(unstableMask),
                    shouldExecute
                )
            }

            irIfThenElse(
                condition = shouldExecute,
                thenPart = irBlock(
                    statements = bodyPreamble.statements + transformed.statements
                ),
                // Use end offsets so that stepping out of the composable function
                // does not step back to the start line for the function.
                elsePart = irSkipToGroupEnd(),
            )
        } else irComposite(
            statements = bodyPreamble.statements + transformed.statements
        )

        scope.realizeGroup(endWithTraceEventEnd)

        declaration.body = context.irFactory.createBlockBody(body.startOffset, body.endOffset).apply {
            this.statements.addAll(
                listOfNotNull(
                    irStartRestartGroup(
                        body,
                        scope,
                        irFunctionSourceKey()
                    ),
                    *scope.markerPreamble.statements.toTypedArray(),
                    *skipPreamble.statements.toTypedArray(),
                    transformedBody,
                    if (returnVar == null) end() else null,
                    returnVar?.let { irReturnVar(declaration.symbol, it) }
                )
            )
        }
        scope.metrics.recordFunction(
            composable = true,
            restartable = true,
            skippable = canSkipExecution,
            isLambda = false,
            inline = false,
            hasDefaults = scope.hasDefaultsGroup,
            readonly = false,
        )

        scope.metrics.recordGroup()

        return declaration
    }

    private fun visitComposableFunctionStub(declaration: IrFunction): IrStatement {
        // remove default parameters as the transform below would
        declaration.parameters.fastForEach { it.defaultValue = null }

        // patch $changed and $default parameters to be the same as passed to the stub
        // stub should always have the form of return Call(...), so we can just match this structure
        val body = declaration.body ?: error("Expected body for composable function stub")
        val call = (body.statements[0] as? IrReturn)?.value as? IrCall ?: error("Expected a single return statement with a call")
        call.symbol.owner.parameters.fastForEach { param ->
            val paramName = param.name.asString()
            if (
                paramName.startsWith(ComposeNames.ChangedParameter.asString()) ||
                paramName.startsWith(ComposeNames.DefaultParameter.asString())
            ) {
                val parameter = declaration.parameters.find { it.name == param.name } ?: error("Expected parameter for ${param.name}")
                call.arguments[param.indexInParameters] = irGet(parameter)
            }
        }

        return declaration
    }

    private fun visitComposableReferenceAdapter(
        declaration: IrFunction,
        scope: Scope.FunctionScope
    ): IrStatement {
        scope.dirty = scope.changedParameter
        scope.preserveIrShape = true

        declaration.transformChildrenVoid()

        return declaration
    }

    private class SourceInfoFixup(val call: IrCall, val index: Int, val scope: Scope.BlockScope)

    private val sourceFixups = mutableListOf<SourceInfoFixup>()

    private fun recordSourceParameter(call: IrCall, index: Int, scope: Scope.BlockScope) {
        sourceFixups.add(SourceInfoFixup(call, index, scope))
    }

    private val (Scope.BlockScope).hasSourceInformation
        get() =
            calculateHasSourceInformation(collectSourceInformation)

    private val (Scope.BlockScope).sourceInformation
        get() =
            calculateSourceInfo(collectSourceInformation)

    private fun applySourceFixups() {
        // Apply the fix-ups lowest scope to highest.
        sourceFixups.sortBy {
            -it.scope.level
        }
        for (sourceFixup in sourceFixups) {
            sourceFixup.call.arguments[sourceFixup.index] = irConst(sourceFixup.scope.sourceInformation ?: "")
        }
        sourceFixups.clear()
    }

    private fun transformDefaults(scope: Scope.FunctionScope): Scope.ParametersScope {
        val parameters = scope.allTrackedParams
        val parametersScope = Scope.ParametersScope()
        parameters.fastForEach { param ->
            val defaultValue = param.defaultValue
            if (defaultValue != null) {
                defaultValue.expression = inScope(parametersScope) {
                    defaultValue.expression.transform(this, null)
                }
            }
        }
        return parametersScope
    }

    private fun buildPreambleStatementsAndReturnIfSkippingPossible(
        sourceElement: IrElement,
        skipPreamble: IrStatementContainer,
        bodyPreamble: IrStatementContainer,
        isSkippableDeclaration: Boolean,
        scope: Scope.FunctionScope,
        dirty: IrChangedBitMaskValue,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
        defaultScope: Scope.ParametersScope,
    ): Boolean {
        val parameters = scope.allTrackedParams
        // we default to true because the absence of a default expression we want to consider as
        // "static"
        val defaultExprIsStatic = BooleanArray(parameters.size) { true }
        val defaultExpr = Array<IrExpression?>(parameters.size) { null }
        val stabilities = Array(parameters.size) { Stability.Unstable }
        var mightSkip = isSkippableDeclaration

        val setDefaults = mutableStatementContainer()
        val skipDefaults = mutableStatementContainer()

        withScope(defaultScope) {
            parameters.fastForEachIndexed { slotIndex, param ->
                val defaultIndex = scope.defaultIndexForSlotIndex(slotIndex)
                val defaultValue = param.defaultValue?.expression
                if (defaultParam != null && defaultValue != null) {

                    // we want to call this on the transformed version.
                    defaultExprIsStatic[slotIndex] = defaultValue.isStatic()
                    defaultExpr[slotIndex] = defaultValue
                    val hasStaticDefaultExpr = defaultExprIsStatic[slotIndex]
                    when {
                        isSkippableDeclaration && !hasStaticDefaultExpr &&
                                dirty is IrChangedBitMaskVariable -> {
                            // If we are setting the parameter to the default expression and
                            // running the default expression again, and the expression isn't
                            // provably static, we can't be certain that the dirty value of
                            // SAME is going to be valid. We must mark it as UNCERTAIN. In order
                            // to avoid slot-table misalignment issues, we must mark it as
                            // UNCERTAIN even when we skip the defaults, so that any child
                            // function receives UNCERTAIN vs SAME/DIFFERENT deterministically.
                            setDefaults.statements.add(
                                irIf(
                                    condition = irGetBit(defaultParam, defaultIndex),
                                    body = irBlock(
                                        statements = listOf(
                                            irSet(param, defaultValue),
                                            dirty.irSetSlotUncertain(slotIndex)
                                        )
                                    )
                                )
                            )
                            skipDefaults.statements.add(
                                irIf(
                                    condition = irGetBit(defaultParam, defaultIndex),
                                    body = dirty.irSetSlotUncertain(slotIndex)
                                )
                            )
                        }
                        else -> {
                            setDefaults.statements.add(
                                irIf(
                                    condition = irGetBit(defaultParam, defaultIndex),
                                    body = irSet(param, defaultValue)
                                )
                            )
                        }
                    }
                }
            }
        }

        parameters.fastForEachIndexed { slotIndex, param ->
            val stability = stabilityInferencer.stabilityOf(param.varargElementType ?: param.type)

            stabilities[slotIndex] = stability

            val isRequired = param.defaultValue == null
            val isUnstable = stability.knownUnstable()
            val isUsed = scope.usedParams[slotIndex]

            scope.metrics.recordParameter(
                declaration = param,
                type = param.type,
                stability = stability,
                default = defaultExpr[slotIndex],
                defaultStatic = defaultExprIsStatic[slotIndex],
                used = isUsed
            )

            if (
                !FeatureFlag.StrongSkipping.enabled &&
                isUsed &&
                isUnstable &&
                isRequired
            ) {
                // if it is a used + unstable parameter with no default expression and we are
                // not in strong skipping mode, the fn will _never_ skip
                mightSkip = false
            }
        }

        // we start the skipPreamble with all of the changed calls. These need to go at the top
        // of the function's group. Note that these end up getting called *before* default
        // expressions, but this is okay because it will only ever get called on parameters that
        // are provided to the function
        parameters.fastForEachIndexed { slotIndex, param ->
            // varargs get handled separately because they will require their own groups
            if (param.isVararg) return@fastForEachIndexed
            val defaultIndex = scope.defaultIndexForSlotIndex(slotIndex)
            val defaultValue = param.defaultValue
            val stability = stabilities[slotIndex]
            val isUnstable = stability.knownUnstable()
            val isUsed = scope.usedParams[slotIndex]

            when {
                !mightSkip || !isUsed -> {
                    // nothing to do
                }
                dirty !is IrChangedBitMaskVariable -> {
                    // this will only ever be true when mightSkip is false, but we put this
                    // branch here so that `dirty` gets smart cast in later branches
                }
                !FeatureFlag.StrongSkipping.enabled && isUnstable && defaultParam != null &&
                        defaultValue != null -> {
                    // if it has a default parameter then the function can still potentially skip
                    skipPreamble.statements.add(
                        irIf(
                            condition = irGetBit(defaultParam, defaultIndex),
                            body = dirty.irOrSetBitsAtSlot(
                                slotIndex,
                                irConst(ParamState.Same.bitsForSlot(slotIndex))
                            )
                        )
                    )
                }
                FeatureFlag.StrongSkipping.enabled || !isUnstable -> {
                    val defaultValueIsStatic = defaultExprIsStatic[slotIndex]
                    val callChanged = irCallChanged(stability, changedParam, slotIndex, param)

                    val isChanged = if (defaultParam != null && !defaultValueIsStatic)
                        irAndAnd(irIsProvided(defaultParam, defaultIndex), callChanged)
                    else
                        callChanged
                    val modifyDirtyFromChangedResult = dirty.irOrSetBitsAtSlot(
                        slotIndex,
                        irIfThenElse(
                            context.irBuiltIns.intType,
                            isChanged,
                            // if the value has changed, update the bits in the slot to be
                            // "Different"
                            thenPart = irConst(ParamState.Different.bitsForSlot(slotIndex)),
                            // if the value has not changed, update the bits in the slot to
                            // be "Same"
                            elsePart = irConst(ParamState.Same.bitsForSlot(slotIndex))
                        )
                    )

                    val skipCondition = if (FeatureFlag.StrongSkipping.enabled)
                        irIsUncertain(changedParam, slotIndex)
                    else
                        irIsUncertainAndStable(changedParam, slotIndex)
                    val stmt = if (defaultParam != null && defaultValue != null && defaultValueIsStatic) {
                        // if the default expression is "static", then we know that if we are using the
                        // default expression, the parameter can be considered "static".
                        irWhen(
                            origin = IrStatementOrigin.IF,
                            branches = listOf(
                                irBranch(
                                    condition = irGetBit(defaultParam, defaultIndex),
                                    result = dirty.irOrSetBitsAtSlot(
                                        slotIndex,
                                        irConst(ParamState.Static.bitsForSlot(slotIndex))
                                    )
                                ),
                                irBranch(
                                    condition = skipCondition,
                                    result = modifyDirtyFromChangedResult
                                )
                            )
                        )
                    } else {
                        // we only call `$composer.changed(...)` on a parameter if the value came in
                        // with an "Uncertain" state AND the value was provided. This is safe to do
                        // because this will remain true or false for *every* execution of the
                        // function, so we will never get a slot table misalignment as a result.
                        irIf(
                            condition = skipCondition,
                            body = modifyDirtyFromChangedResult
                        )
                    }
                    skipPreamble.statements.add(stmt)
                }
            }
        }

        // now we handle the vararg parameters specially since it needs to create a group
        parameters.fastForEachIndexed { slotIndex, param ->
            val varargElementType = param.varargElementType ?: return@fastForEachIndexed
            if (mightSkip && dirty is IrChangedBitMaskVariable) {
                // for vararg parameters of stable type, we can store each value in the slot
                // table, but need to generate a group since the size of the array could change
                // over time. In the future, we may want to make an optimization where whether or
                // not the call site had a spread or not and only create groups if it did.

                // for varargs with default type, check if $default is set for that parameter
                val statements = if (defaultParam != null && param.defaultValue != null) {
                    val defaultIndex = scope.defaultIndexForSlotIndex(slotIndex)
                    val block = irBlock(statements = listOf())
                    skipPreamble.statements.add(
                        irIf(
                            condition = irIsProvided(defaultParam, defaultIndex),
                            body = block
                        )
                    )
                    block.statements
                } else {
                    skipPreamble.statements
                }

                // composer.startMovableGroup(<>, values.size)
                val sizeGetter = param.type.classOrNull!!.getPropertyGetter("size")!!.owner
                val irGetParamSize = irMethodCall(
                    irGet(param),
                    sizeGetter
                )

                statements.add(
                    irStartMovableGroup(
                        param,
                        irGetParamSize,
                        defaultScope,
                    )
                )

                // dirty = if (composer.changed(values.length)) 0b0100 else 0b0000
                // for (value in values) {
                //     dirty = dirty or if (composer.changed(value)) 0b0100 else 0b0000
                // }
                statements.add(
                    dirty.irOrSetBitsAtSlot(
                        slotIndex,
                        irIfThenElse(
                            context.irBuiltIns.intType,
                            irChanged(irMethodCall(irGet(param), sizeGetter), compareInstanceForFunctionTypes = true),
                            thenPart = irConst(ParamState.Different.bitsForSlot(slotIndex)),
                            elsePart = irConst(ParamState.Uncertain.bitsForSlot(slotIndex))
                        )
                    )
                )
                statements.add(
                    irForLoop(
                        varargElementType,
                        irGet(param)
                    ) { loopVar ->
                        val changedCall = irCallChanged(
                            stabilityInferencer.stabilityOf(varargElementType),
                            changedParam,
                            slotIndex,
                            loopVar
                        )

                        dirty.irOrSetBitsAtSlot(
                            slotIndex,
                            irIfThenElse(
                                context.irBuiltIns.intType,
                                changedCall,
                                // if the value has changed, update the bits in the slot to be
                                // "Different".
                                thenPart = irConst(ParamState.Different.bitsForSlot(slotIndex)),
                                // if the value has not changed, we are still uncertain if the entire
                                // list of values has gone unchanged or not, so we use Uncertain
                                elsePart = irConst(ParamState.Uncertain.bitsForSlot(slotIndex))
                            )
                        )
                    }
                )

                // composer.endMovableGroup()
                statements.add(irEndMovableGroup(scope))

                // if (dirty and 0b1110 === 0) {
                //   dirty = dirty or 0b0010
                // }
                statements.add(
                    irIf(
                        condition = irIsUncertainAndStable(dirty, slotIndex),
                        body = dirty.irOrSetBitsAtSlot(
                            slotIndex,
                            irConst(ParamState.Same.bitsForSlot(slotIndex))
                        )
                    )
                )
            }
        }
        parameters.fastForEach {
            // we want to remove the default expression from the function. This will prevent
            // the kotlin compiler from doing its own default handling, which we don't need.
            it.defaultValue = null
        }
        // after all of this, we need to potentially wrap the default setters in a group and if
        // statement, to make sure that defaults are only executed when they need to be.
        if (!mightSkip || defaultExprIsStatic.all { it }) {
            // if we don't skip execution ever, then we don't need these groups at all.
            // Additionally, if all of the defaults are static, we can avoid creating the groups
            // as well.
            // NOTE(lmr): should we still wrap this in an if statement to be safe???
            bodyPreamble.statements.addAll(setDefaults.statements)
        } else if (setDefaults.statements.isNotEmpty()) {
            // otherwise, we wrap the whole thing in an if expression with a skip
            scope.hasDefaultsGroup = true
            scope.metrics.recordGroup()
            bodyPreamble.statements.add(irStartDefaults(sourceElement, defaultScope))
            bodyPreamble.statements.add(
                irIfThenElse(
                    // this prevents us from re-executing the defaults if this function is getting
                    // executed from a recomposition
                    // if (%changed and 0b0001 == 0 || %composer.defaultsInvalid) {
                    condition = irOrOr(
                        irEqual(changedParam.irLowBit(), irConst(0)),
                        irDefaultsInvalid()
                    ),
                    // set all of the default temp vars
                    thenPart = setDefaults,
                    // composer.skipCurrentGroup()
                    elsePart = irBlock(
                        statements = listOf(
                            irSkipToGroupEnd(),
                            *skipDefaults.statements.toTypedArray()
                        )
                    )
                )
            )
            bodyPreamble.statements.add(irEndDefaults())
        }

        return mightSkip
    }

    private fun irCallChanged(
        stability: Stability,
        changedParam: IrChangedBitMaskValue,
        slotIndex: Int,
        param: IrValueDeclaration,
    ) = if (FeatureFlag.StrongSkipping.enabled && stability.isUncertain()) {
        irIfThenElse(
            type = context.irBuiltIns.booleanType,
            condition = irIsStable(changedParam, slotIndex),
            thenPart = irChanged(
                irCurrentComposer(),
                irGet(param),
                inferredStable = true,
                compareInstanceForFunctionTypes = true,
                compareInstanceForUnstableValues = true
            ),
            elsePart = irChanged(
                irCurrentComposer(),
                irGet(param),
                inferredStable = false,
                compareInstanceForFunctionTypes = true,
                compareInstanceForUnstableValues = true
            )
        )
    } else {
        irChanged(
            irGet(param),
            compareInstanceForFunctionTypes = true
        )
    }

    private fun irEndRestartGroupAndUpdateScope(
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
        numRealValueParameters: Int,
    ): IrExpression {
        val function = scope.function

        // Save the dispatch receiver into a temporary created in
        // the outer scope because direct references to the
        // receiver sometimes cause an invalid name, "$<this>", to
        // be generated.
        // todo check if that's still the case
        val outerReceiver = function.dispatchReceiverParameter?.let {
            irTemporary(
                value = irGet(it),
                nameHint = "rcvr"
            )
        }

        // Create self-invoke lambda
        val parameterCount = function.parameters.size
        val composerIndex = numRealValueParameters + function.thisParamCount
        val changedIndex = composerIndex + 1
        val defaultIndex = changedIndex + changedParamCount(
            numRealValueParameters,
            function.thisParamCount
        )

        if (defaultParam == null) {
            // param count is 1-based, index is 0-based
            require(parameterCount == defaultIndex) {
                "Expected $defaultIndex params for ${function.fqNameWhenAvailable}, " +
                        "found $parameterCount"
            }
        } else {
            val expectedParamCount = defaultIndex + defaultParamCount(numRealValueParameters)
            require(parameterCount == expectedParamCount) {
                "Expected $expectedParamCount params for ${function.fqNameWhenAvailable}, " +
                        "found $parameterCount"
            }
        }

        val lambda = irLambdaExpression(
            startOffset = UNDEFINED_OFFSET,
            endOffset = UNDEFINED_OFFSET,
            returnType = builtIns.unitType
        ) { fn ->
            fn.parent = function
            val newComposer = fn.addValueParameter(
                ComposeNames.ComposerParameter.identifier,
                composerIrClass.defaultType
                    .replaceArgumentsWithStarProjections()
                    .makeNullable()
            )
            fn.addValueParameter(
                ComposeNames.ForceParameter,
                builtIns.intType
            )
            fn.body = DeclarationIrBuilder(context, fn.symbol).irBlockBody {
                // Call the function again with the same parameters
                +irReturn(
                    irCall(function.symbol).apply {
                        symbol.owner.parameters.fastForEach { param ->
                            arguments[param.indexInParameters] =
                                if (param.kind == IrParameterKind.DispatchReceiver) {
                                    outerReceiver?.let(::irGet)
                                } else {
                                    irGet(param)
                                }
                        }

                        // new composer
                        arguments[composerIndex] = irGet(newComposer)

                        // the call in updateScope needs to *always* have the low bit set to 1.
                        // This ensures that the body of the function is actually executed.
                        changedParam.putAsValueArgumentInWithLowBit(
                            this,
                            changedIndex,
                            lowBit = true
                        )

                        defaultParam?.putAsValueArgumentIn(this, defaultIndex)

                        function.typeParameters.fastForEachIndexed { index, parameter ->
                            typeArguments[index] = parameter.defaultType
                        }
                    }
                )
            }
        }

        // $composer.endRestartGroup()?.updateScope { next, _ -> TheFunction(..., next) }
        return irBlock(
            statements = listOfNotNull(
                outerReceiver,
                irSafeCall(
                    irEndRestartGroup(scope),
                    updateScopeFunction.symbol,
                    lambda
                ),
            )
        )
    }

    fun irCurrentMarker(composerParameter: IrValueParameter) =
        irMethodCall(
            irCurrentComposer(composerParameter = composerParameter),
            currentMarkerProperty!!.getter!!
        )

    private fun irIsSkipping() =
        irMethodCall(irCurrentComposer(), isSkippingFunction.getter!!)

    private fun irShouldExecute(parametersChanged: IrExpression, flags: IrExpression): IrExpression {
        val shouldExecuteFunction = shouldExecuteFunction
        return if (shouldExecuteFunction != null) {
            irMethodCall(irCurrentComposer(), shouldExecuteFunction).apply {
                // 0th is receiver
                arguments[1] = parametersChanged
                arguments[2] = flags
            }
        } else {
            irOrOr(
                parametersChanged,
                irNot(irIsSkipping())
            )
        }
    }

    private fun irDefaultsInvalid() =
        irMethodCall(irCurrentComposer(), defaultsInvalidFunction.getter!!)

    private fun irIsProvided(default: IrDefaultBitMaskValue, slot: Int) =
        irEqual(default.irIsolateBitAtIndex(slot), irConst(0))

    // %changed and 0b111 == 0
    private fun irIsUncertainAndStable(changed: IrChangedBitMaskValue, slot: Int) = irEqual(
        changed.irIsolateBitsAtSlot(slot, includeStableBit = true),
        irConst(0)
    )

    private fun irIsStable(changed: IrChangedBitMaskValue, slot: Int) = irEqual(
        changed.irStableBitAtSlot(slot),
        irConst(0)
    )

    private fun irIsUncertain(changed: IrChangedBitMaskValue, slot: Int) = irEqual(
        changed.irIsolateBitsAtSlot(slot, includeStableBit = false),
        irConst(0)
    )

    @Suppress("SameParameterValue")
    private fun irBitsForSlot(bits: Int, slot: Int): IrExpression {
        return irConst(bitsForSlot(bits, slot))
    }

    private fun IrExpression.endsWithReturnOrJump(): Boolean {
        var expr: IrStatement? = this
        while (expr != null) {
            if (expr is IrReturn) return true
            if (expr is IrBreakContinue) return true
            if (expr !is IrBlock) return false
            expr = expr.statements.lastOrNull()
        }
        return false
    }

    private fun IrContainerExpression.wrapWithTraceEvents(
        key: IrExpression,
        scope: Scope.FunctionScope,
    ) {
        val start = irTraceEventStart(key, scope)
        val end = irTraceEventEnd()
        if (start != null && end != null) {
            statements.add(0, start)
            statements.add(end)
        }
    }

    private fun IrBody.asBodyAndResultVar(
        expectedTarget: IrFunction? = null,
    ): Pair<IrContainerExpression, IrVariable?> {
        val original = IrCompositeImpl(
            startOffset,
            endOffset,
            context.irBuiltIns.unitType,
            null,
            statements
        )
        var block: IrStatementContainer? = original
        var expr: IrStatement? = block?.statements?.lastOrNull()
        while (expr != null && block != null) {
            if (
                expr is IrReturn &&
                (expectedTarget == null || expectedTarget == expr.returnTargetSymbol.owner)
            ) {
                block.statements.pop()
                val valueType = expr.value.type
                val returnType = (expr.returnTargetSymbol as? IrFunctionSymbol)?.owner?.returnType
                    ?: valueType
                return if (returnType.isUnit() || returnType.isNothing() || valueType.isNothing()) {
                    block.statements.add(expr.value)
                    original to null
                } else {
                    val temp = irTemporary(expr.value)
                    block.statements.add(temp)
                    original to temp
                }
            }
            if (expr !is IrBlock)
                return original to null
            block = expr
            expr = block.statements.lastOrNull()
        }
        return original to null
    }

    override fun visitProperty(declaration: IrProperty): IrStatement =
        inScope(Scope.PropertyScope(declaration.name)) {
            super.visitProperty(declaration)
        }

    override fun visitField(declaration: IrField): IrStatement =
        inScope(Scope.FieldScope(declaration.name)) {
            super.visitField(declaration)
        }

    override fun visitFile(declaration: IrFile): IrFile =
        includeFileNameInExceptionTrace(declaration) {
            inScope(Scope.FileScope(declaration)) {
                super.visitFile(declaration)
            }
        }

    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        when (declaration) {
            is IrField,
            is IrProperty,
            is IrFunction,
            is IrClass -> {
                // these declarations get scopes, but they are handled individually
                return super.visitDeclaration(declaration)
            }
            is IrTypeAlias,
            is IrEnumEntry,
            is IrAnonymousInitializer,
            is IrTypeParameter,
            is IrLocalDelegatedProperty,
            is IrValueDeclaration,
            is IrScript -> {
                // these declarations do not create new "scopes", so we do nothing
                return super.visitDeclaration(declaration)
            }
            else -> error("Unhandled declaration! ${declaration::class.java.simpleName}")
        }
    }

    private fun nearestComposer(): IrValueParameter = currentScope.myComposer

    private fun irCurrentComposer(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        composerParameter: IrValueParameter = nearestComposer(),
    ): IrExpression {
        return IrGetValueImpl(
            startOffset,
            endOffset,
            composerParameter.symbol
        )
    }

    private fun Scope.BlockScope.irCurrentComposer(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrExpression =
        irCurrentComposer(startOffset, endOffset, nearestComposer ?: nearestComposer())

    private fun IrElement.sourceKey(): Int {
        var hash = functionSourceKey(currentFunctionScope.function)
        hash = 31 * hash + (startOffset - currentFunctionScope.function.startOffset)
        hash = 31 * hash + (endOffset - currentFunctionScope.function.startOffset)


        when (this) {
            // Disambiguate ?. clauses which become a "null" constant expression
            is IrConst -> {
                hash = 31 * hash + (this.value?.hashCode() ?: 1)
            }
            // Disambiguate the key for blocks and composite containers in case block offsets are
            // the same as its contents
            is IrBlock -> {
                hash = 31 * hash + 2
            }
            is IrComposite -> {
                hash = 31 * hash + 3
            }
        }

        return hash
    }

    private fun functionSourceKey(function: IrFunction): Int {
        when (val fn = function) {
            is IrSimpleFunction -> {
                return fn.sourceKey()
            }
            is IrConstructor -> {
                error("expected simple function, got constructor")
            }
        }
    }

    private fun IrElement.irSourceKey(): IrConst =
        IrConstImpl.int(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.intType,
            sourceKey()
        )

    private fun irFunctionSourceKey(function: IrFunction = currentFunctionScope.function): IrConst =
        IrConstImpl.int(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.intType,
            functionSourceKey(function)
        )

    private fun irStartReplaceGroup(
        element: IrElement,
        scope: Scope.BlockScope,
        key: IrExpression = element.irSourceKey(),
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
    ): IrExpression {
        return irWithSourceInformation(
            irStartReplaceGroup(
                scope.irCurrentComposer(startOffset, endOffset),
                key,
                startOffset,
                endOffset
            ),
            scope
        )
    }

    private fun irWithSourceInformation(
        startGroup: IrExpression,
        scope: Scope.BlockScope,
    ): IrExpression {
        return if (collectSourceInformation && scope.hasSourceInformation) {
            irBlock(statements = listOf(startGroup, irSourceInformation(scope)))
        } else startGroup
    }

    private fun irSourceInformation(scope: Scope.BlockScope): IrExpression {
        val sourceInformation = irCall(
            sourceInformationFunction
        ).also {
            it.arguments[0] = scope.irCurrentComposer()
        }
        recordSourceParameter(sourceInformation, 1, scope)
        return sourceInformation
    }

    private fun irSourceInformationMarkerStart(
        element: IrElement,
        scope: Scope.BlockScope,
        key: IrExpression = element.irSourceKey(),
    ): IrExpression {
        return irCall(
            sourceInformationMarkerStartFunction,
            element.startOffset,
            element.endOffset
        ).also {
            it.arguments[0] = scope.irCurrentComposer()
            it.arguments[1] = key
            recordSourceParameter(it, 2, scope)
        }
    }

    private fun irSourceInformationMarkerEnd(
        element: IrElement,
        scope: Scope.BlockScope,
    ): IrExpression {
        return irCall(
            sourceInformationMarkerEndFunction,
            element.startOffset,
            element.endOffset
        ).also {
            it.arguments[0] = scope.irCurrentComposer()
        }
    }

    private fun irWithSourceInformationMarker(
        expression: IrExpression,
        scope: Scope.BlockScope,
        before: List<IrStatement>,
    ): IrExpression = if (collectSourceInformation && scope.hasSourceInformation) {
        expression.wrap(
            before = before + listOf(irSourceInformationMarkerStart(expression, scope)),
            after = listOf(irSourceInformationMarkerEnd(expression, scope))
        )
    } else if (before.isNotEmpty())
        expression.wrap(before = before)
    else expression

    private fun irIsTraceInProgress(): IrExpression? =
        isTraceInProgressFunction?.let { irCall(it) }

    private fun irIfTraceInProgress(body: IrExpression): IrExpression? =
        irIsTraceInProgress()?.let { isTraceInProgress ->
            irIf(isTraceInProgress, body)
        }

    private fun irTraceEventStart(key: IrExpression, scope: Scope.FunctionScope): IrExpression? =
        traceEventStartFunction?.let { traceEventStart ->
            val declaration = scope.function
            val startOffset = declaration.body!!.startOffset

            val name = declaration.kotlinFqName
            val file = declaration.file.name
            // FIXME: This should probably use `declaration.startOffset`, but the K2 implementation
            //        is unfinished (i.e., in K2 the start offset of an annotated function could
            //        point at the annotation instead of the start of the function).
            val line = declaration.file.fileEntry.getLineNumber(startOffset)
            val traceInfo = "$name ($file:$line)" // TODO(174715171) decide on what to log
            val dirty = scope.dirty
            val changed = scope.changedParameter
            val params = if (dirty != null && dirty.used)
                dirty.declarations
            else
                changed?.declarations
            val dirty1 = params?.getOrNull(0)?.let { irGet(it) } ?: irConst(-1)
            val dirty2 = params?.getOrNull(1)?.let { irGet(it) } ?: irConst(-1)

            irIfTraceInProgress(
                irCall(traceEventStart).also {
                    it.arguments[0] = key
                    it.arguments[1] = dirty1
                    it.arguments[2] = dirty2
                    it.arguments[3] = irConst(traceInfo)
                }
            )
        }

    private fun irTraceEventEnd(): IrExpression? =
        traceEventEndFunction?.let {
            irIfTraceInProgress(irCall(it))
        }

    private fun irStartDefaults(element: IrElement, scope: Scope.BlockScope): IrExpression {
        return irWithSourceInformation(
            irMethodCall(
                irCurrentComposer(),
                startDefaultsFunction,
                element.startOffset,
                element.endOffset
            ),
            scope
        )
    }

    private fun irStartRestartGroup(
        element: IrElement,
        scope: Scope.BlockScope,
        key: IrExpression = element.irSourceKey(),
    ): IrExpression {
        return irWithSourceInformation(
            irSet(
                nearestComposer(),
                irMethodCall(
                    scope.irCurrentComposer(),
                    startRestartGroupFunction,
                    element.startOffset,
                    element.endOffset
                ).also {
                    // 0th is the receiver
                    it.arguments[1] = key
                }
            ),
            scope
        )
    }

    private fun irEndRestartGroup(scope: Scope.BlockScope): IrExpression {
        // This is a workaround for d8 generating duplicate line number entries
        // whenever a branch without a line number is emitted by the compiler.
        // skipToGroupEnd now points to the first line of the function, so every
        // calls after that should explicitly set the line number to the end of the function.
        // see b/415337077#26, b/417412949
        val offset = (scope as? Scope.FunctionScope)?.function?.endOffset ?: UNDEFINED_OFFSET
        return irMethodCall(
            scope.irCurrentComposer(offset, offset),
            endRestartGroupFunction,
            offset,
            offset
        )
    }

    private fun irChanged(
        value: IrExpression,
        compareInstanceForFunctionTypes: Boolean,
        compareInstanceForUnstableValues: Boolean = FeatureFlag.StrongSkipping.enabled,
    ): IrExpression = irChanged(
        irCurrentComposer(),
        value,
        inferredStable = false,
        compareInstanceForFunctionTypes = compareInstanceForFunctionTypes,
        compareInstanceForUnstableValues = compareInstanceForUnstableValues
    )

    private fun irSkipToGroupEnd(
        // This is a workaround for d8 generating duplicate line number entries
        // whenever a branch without a line number is emitted by the compiler.
        // We are now setting it to the first line in the function which should
        // point to a location that does not interfere with the function body.
        // see b/415337077#26, b/417412949
        startOffset: Int = currentFunctionScope.function.startOffset,
        endOffset: Int = currentFunctionScope.function.startOffset
    ): IrExpression {
        return irMethodCall(
            irCurrentComposer(startOffset, endOffset),
            skipToGroupEndFunction,
            startOffset,
            endOffset
        )
    }

    private fun irEndReplaceGroup(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        scope: Scope.BlockScope,
    ): IrExpression {
        return irEndReplaceGroup(
            scope.irCurrentComposer(startOffset, endOffset),
            startOffset,
            endOffset
        )
    }

    private fun irEndDefaults(): IrExpression {
        return irMethodCall(irCurrentComposer(), endDefaultsFunction)
    }

    private fun irStartMovableGroup(
        element: IrElement,
        joinedData: IrExpression,
        scope: Scope.BlockScope,
    ): IrExpression {
        return irWithSourceInformation(
            irMethodCall(
                scope.irCurrentComposer(),
                startMovableFunction,
                element.startOffset,
                element.endOffset
            ).also {
                // 0th is the receiver
                it.arguments[1] = element.irSourceKey()
                it.arguments[2] = joinedData
            },
            scope
        )
    }

    private fun irEndMovableGroup(scope: Scope.BlockScope): IrExpression {
        return irMethodCall(scope.irCurrentComposer(), endMovableFunction)
    }

    private fun irEndToMarker(marker: IrExpression, scope: Scope.BlockScope): IrExpression {
        return irMethodCall(scope.irCurrentComposer(), endToMarkerFunction!!).apply {
            // 0th is the receiver
            arguments[1] = marker
        }
    }

    private fun irJoinKeyChain(keyExprs: List<IrExpression>): IrExpression {
        return keyExprs.reduce { accumulator, value ->
            irMethodCall(irCurrentComposer(), joinKeyFunction).apply {
                // 0th is the receiver
                arguments[1] = accumulator
                arguments[2] = value
            }
        }
    }

    private fun irSafeCall(
        target: IrExpression,
        symbol: IrFunctionSymbol,
        vararg args: IrExpression,
    ): IrExpression {
        val tmpVal = irTemporary(target, nameHint = "safe_receiver")
        return irBlock(
            origin = IrStatementOrigin.SAFE_CALL,
            statements = listOf(
                tmpVal,
                irIfThenElse(
                    condition = irEqual(irGet(tmpVal), irNull()),
                    thenPart = irNull(),
                    elsePart = irCall(
                        symbol = symbol,
                        dispatchReceiver = irGet(tmpVal),
                        args = args
                    )
                )
            )
        )
    }

    private fun irTemporary(
        value: IrExpression,
        nameHint: String? = null,
        irType: IrType = value.type,
        isVar: Boolean = false,
        exactName: Boolean = false,
    ): IrVariableImpl {
        val scope = currentFunctionScope
        val name = if (exactName && nameHint != null)
            nameHint
        else
            scope.getNameForTemporary(nameHint)
        return irTemporary(
            value,
            name,
            irType,
            isVar
        ).also {
            it.parent = currentFunctionScope.function.parent
        }
    }

    private fun IrBlock.withReplaceGroupStatements(
        scope: Scope.BlockScope,
        insertAt: Int = 0,
    ): IrExpression {
        currentFunctionScope.metrics.recordGroup()
        scope.realizeGroup {
            irEndReplaceGroup(scope = scope)
        }

        val prefix = statements.subList(0, insertAt)
        val suffix = statements.subList(insertAt, statements.size)
        return when {
            // if the scope ends with a return call, then it will get properly ended if we
            // just push the end call on the scope because of the way returns get transformed in
            // this class. As a result, here we can safely just "prepend" the start call
            endsWithReturnOrJump() -> IrBlockImpl(
                startOffset,
                endOffset,
                type,
                origin,
                prefix + listOf(irStartReplaceGroup(this, scope)) + suffix
            )
            // otherwise, we want to push an end call for any early returns/jumps, but also add
            // an end call to the end of the group
            else -> IrBlockImpl(
                startOffset,
                endOffset,
                type,
                origin,
                prefix + listOf(
                    irStartReplaceGroup(
                        this,
                        scope,
                        startOffset = startOffset,
                        endOffset = endOffset
                    )
                ) + suffix + listOf(irEndReplaceGroup(startOffset, endOffset, scope))
            )
        }
    }

    private fun IrExpression.asReplaceGroup(scope: Scope.BlockScope): IrExpression {
        currentFunctionScope.metrics.recordGroup()
        // if the scope has no composable calls, then the only important thing is that a
        // start/end call gets executed. as a result, we can just put them both at the top of
        // the group, and we don't have to deal with any of the complicated jump logic that
        // could be inside of the block
        if (!scope.hasComposableCalls && !scope.hasReturn && !scope.hasJump) {
            return wrap(
                before = listOf(
                    irStartReplaceGroup(
                        this,
                        scope,
                        startOffset = startOffset,
                        endOffset = endOffset,
                    ),
                    irEndReplaceGroup(startOffset, endOffset, scope)
                )
            )
        }
        scope.realizeGroup {
            irEndReplaceGroup(scope = scope)
        }
        return when {
            // if the scope ends with a return call, then it will get properly ended if we
            // just push the end call on the scope because of the way returns get transformed in
            // this class. As a result, here we can safely just "prepend" the start call
            endsWithReturnOrJump() -> {
                wrap(before = listOf(irStartReplaceGroup(this, scope)))
            }
            // otherwise, we want to push an end call for any early returns/jumps, but also add
            // an end call to the end of the group
            else -> {
                wrap(
                    before = listOf(
                        irStartReplaceGroup(
                            this,
                            scope,
                            startOffset = startOffset,
                            endOffset = endOffset
                        )
                    ),
                    after = listOf(irEndReplaceGroup(startOffset, endOffset, scope))
                )
            }
        }
    }

    private fun IrExpression.variablePrefix(variable: IrVariable) =
        IrBlockImpl(
            startOffset,
            endOffset,
            type,
            null,
            listOf(variable, this)
        )

    fun IrExpression.wrap(
        before: List<IrStatement> = emptyList(),
        after: List<IrStatement> = emptyList(),
    ): IrContainerExpression {
        return if (after.isEmpty() || type.isNothing() || type.isUnit()) {
            wrap(startOffset, endOffset, type, before, after)
        } else {
            val tmpVar = irTemporary(this, nameHint = "group")
            tmpVar.wrap(
                startOffset,
                endOffset,
                type,
                before,
                after + irGet(tmpVar)
            )
        }
    }

    private fun IrExpression.asCoalescableGroup(scope: Scope.BlockScope): IrExpression {
        val metrics = currentFunctionScope.metrics
        val before = mutableStatementContainer()
        val after = mutableStatementContainer()

        // Since this expression produces a dynamic number of groups, we may need to wrap it with
        // a group directly. We don't know that for sure yet, so we provide the parent scope with
        // handlers to do that if it ends up needing to.
        encounteredCoalescableGroup(
            scope,
            realizeGroup = {
                if (before.statements.isEmpty()) {
                    metrics.recordGroup()
                    before.statements.add(irStartReplaceGroup(this, scope))
                    after.statements.add(irEndReplaceGroup(scope = scope))
                }
            },
            makeEnd = {
                irEndReplaceGroup(scope = scope)
            }
        )
        return wrap(
            listOf(before),
            listOf(after)
        )
    }

    private fun IrContainerExpression.asSourceOrEarlyExitGroup(
        scope: Scope.FunctionScope,
    ): IrContainerExpression {
        val needsGroup = scope.hasInlineEarlyReturn || scope.isCrossinlineLambda
        if (needsGroup) {
            currentFunctionScope.metrics.recordGroup()
        } else if (!collectSourceInformation) {
            // If we are not generating source information and the lambda does not contain an
            // early exit this we don't need a group or source markers.
            return this
        }
        // if the scope has no composable calls, then the only important thing is that a
        // start/end call gets executed. as a result, we can just put them both at the top of
        // the group, and we don't have to deal with any of the complicated jump logic that
        // could be inside of the block
        val makeStart = {
            if (needsGroup) irStartReplaceGroup(
                this,
                scope,
                startOffset = startOffset,
                endOffset = endOffset
            )
            else irSourceInformationMarkerStart(this, scope)
        }
        val makeEnd = {
            if (needsGroup) irEndReplaceGroup(scope = scope)
            else irSourceInformationMarkerEnd(this, scope)
        }
        if (!scope.hasComposableCalls && !scope.hasReturn && !scope.hasJump) {
            return wrap(
                before = listOf(makeStart()),
                after = listOf(makeEnd()),
            )
        }

        scope.realizeGroup(makeEnd)
        return when {
            // if the scope ends with a return call, then it will get properly ended if we
            // just push the end call on the scope because of the way returns get transformed in
            // this class. As a result, here we can safely just "prepend" the start call
            endsWithReturnOrJump() -> {
                wrap(before = listOf(makeStart()))
            }
            // otherwise, we want to push an end call for any early returns/jumps, but also add
            // an end call to the end of the group
            else -> {
                wrap(
                    before = listOf(makeStart()),
                    after = listOf(makeEnd()),
                )
            }
        }
    }

    private fun mutableStatementContainer() = mutableStatementContainer(context)

    private fun encounteredComposableCall(withGroups: Boolean) {
        var scope: Scope? = currentScope
        // it is important that we only report "withGroups: false" for the _nearest_ scope, and
        // every scope above that it effectively means there was a group even if it is false
        var groups = withGroups
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    scope.recordComposableCall(groups)
                    groups = true
                    if (!scope.isInlinedLambda) {
                        break@loop
                    }
                }
                is Scope.BlockScope -> {
                    scope.recordComposableCall(groups)
                    groups = true
                }
                is Scope.ClassScope -> {
                    break@loop
                }
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
    }

    private fun recordCallInSource(call: IrElement) {
        var scope: Scope? = currentScope
        var location: Scope.SourceLocation? = null
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    location = scope.recordSourceLocation(call, location)
                }
                is Scope.BlockScope -> {
                    location = scope.recordSourceLocation(call, location)
                }
                is Scope.ClassScope ->
                    break@loop
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
    }

    private fun encounteredCapturedComposableCall() {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.CaptureScope -> {
                    scope.markCapturedComposableCall()
                    break@loop
                }
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
    }

    private fun encounteredCoalescableGroup(
        coalescableScope: Scope.BlockScope,
        realizeGroup: () -> Unit,
        makeEnd: () -> IrExpression,
    ) {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.CallScope,
                is Scope.ReturnScope -> {
                    // Ignore
                }
                is Scope.FunctionScope -> {
                    scope.markCoalescableGroup(coalescableScope, realizeGroup, makeEnd)
                    if (!scope.isInlinedLambda || scope.isComposable) {
                        break@loop
                    }
                }
                is Scope.BlockScope -> {
                    scope.markCoalescableGroup(coalescableScope, realizeGroup, makeEnd)
                    break@loop
                }
                else -> error("Unexpected scope type")
            }
            scope = scope.parent
        }
    }

    private fun encounteredReturn(
        symbol: IrReturnTargetSymbol,
        extraEndLocation: (IrExpression) -> Unit,
    ) {
        var scope: Scope? = currentScope
        val blockScopeMarks = mutableListOf<Scope.BlockScope>()
        var leavingInlinedLambda = false
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.function == symbol.owner) {
                        scope.hasAnyEarlyReturn = true
                        if (!leavingInlinedLambda || !rollbackGroupMarkerEnabled) {
                            blockScopeMarks.fastForEach {
                                it.markReturn(extraEndLocation)
                            }
                            scope.markReturn(extraEndLocation)
                            if (scope.isInlinedLambda && scope.inComposableCall) {
                                scope.hasInlineEarlyReturn = true
                            }
                        } else {
                            val functionScope = scope
                            val targetScope = currentScope as? Scope.BlockScope ?: functionScope
                            val marker = irGet(functionScope.allocateMarker())
                            extraEndLocation(irEndToMarker(marker, targetScope))
                            if (functionScope.isInlinedLambda) {
                                scope.hasInlineEarlyReturn = true
                            } else {
                                functionScope.markReturn(extraEndLocation)
                            }
                        }
                        break@loop
                    }
                    if (scope.isInlinedLambda && scope.inComposableCall) {
                        leavingInlinedLambda = true
                        scope.hasInlineEarlyReturn = true
                    }
                }
                is Scope.BlockScope -> {
                    blockScopeMarks.add(scope)
                }
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
    }

    private fun encounteredJump(jump: IrBreakContinue, extraEndLocation: (IrExpression) -> Unit) {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.ClassScope -> error("Unexpected Class Scope encountered")
                is Scope.FunctionScope -> {
                    if (!scope.isInlinedLambda) {
                        error("Unexpected Function Scope encountered")
                    }
                }
                is Scope.LoopScope -> {
                    scope.markJump(jump, extraEndLocation)
                    if (jump.loop == scope.loop) break@loop
                }
                is Scope.BlockScope -> {
                    scope.markJump(extraEndLocation)
                }
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
    }

    private fun <T : Scope> IrExpression.transformWithScope(scope: T): Pair<T, IrExpression> {
        val previousScope = currentScope
        try {
            currentScope = scope
            scope.parent = previousScope
            scope.level = previousScope.level + 1
            val result = transform(this@ComposableFunctionBodyTransformer, null)
            return scope to result
        } finally {
            currentScope = previousScope
        }
    }

    private inline fun <T : Scope> withScope(scope: T, block: () -> Unit): T {
        val previousScope = currentScope
        currentScope = scope
        scope.parent = previousScope
        scope.level = previousScope.level + 1
        try {
            block()
        } finally {
            currentScope = previousScope
        }
        return scope
    }

    private inline fun <R> inScope(scope: Scope, block: () -> R): R {
        val previousScope = currentScope
        currentScope = scope
        scope.parent = previousScope
        scope.level = previousScope.level + 1
        try {
            return block()
        } finally {
            currentScope = previousScope
        }
    }

    private inline fun Scope.forEach(crossinline block: (scope: Scope) -> Unit) {
        var current: Scope? = this
        while (current != null) {
            block(current)
            current = current.parent
        }
    }

    /**
     * Argument information extracted from the call site and argument expression itself.
     */
    data class CallArgumentMeta(
        /** stability of argument expression */
        var stability: Stability = Stability.Unstable,
        /** whether argument is vararg */
        var isVararg: Boolean = false,
        /** whether default value for the arg is provided */
        var isProvided: Boolean = false,
        /** whether the expression is static */
        var isStatic: Boolean = false,
        /** metadata from enclosing function parameters (NOT the function being called) */
        var paramRef: ParamMeta? = null,
    ) {
        val isCertain get() = paramRef != null
    }

    /**
     * Composable call information extracted from composable function parameters referenced
     * in a call argument.
     */
    data class ParamMeta(
        /** Slot index in maskParam */
        val maskSlot: Int = -1,
        /** Reference to $changed or $dirty parameter with the [ParamState] mask */
        var maskParam: IrChangedBitMaskValue? = null,
        /** Whether the parameter has a non-static default value. */
        val hasNonStaticDefault: Boolean = false,
    )

    private fun argumentMetaOf(arg: IrExpression, isProvided: Boolean): CallArgumentMeta {
        val meta = CallArgumentMeta(isProvided = isProvided)
        populateArgumentMeta(arg, meta)
        return meta
    }

    private fun populateArgumentMeta(arg: IrExpression, meta: CallArgumentMeta) {
        meta.stability = stabilityInferencer.stabilityOf(arg)
        when {
            arg.isStatic() -> meta.isStatic = true
            arg is IrGetValue -> {
                when (val owner = arg.symbol.owner) {
                    is IrValueParameter -> {
                        meta.paramRef = extractParamMetaFromScopes(owner)
                    }
                    is IrVariable -> {
                        if (owner.isConst) {
                            meta.isStatic = true
                        } else if (!owner.isVar && owner.initializer != null) {
                            populateArgumentMeta(owner.initializer!!, meta)
                        }
                    }
                }
            }
            arg is IrVararg -> {
                meta.stability = stabilityInferencer.stabilityOf(arg.varargElementType)
            }
        }
    }

    private fun extractParamMetaFromScopes(param: IrValueDeclaration): ParamMeta? {
        var scope: Scope? = currentScope
        val fn = param.parent
        while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.function == fn) {
                        if (scope.isComposable) {
                            val slotIndex = scope.allTrackedParams.indexOf(param)
                            if (slotIndex != -1) {
                                return ParamMeta(
                                    maskSlot = slotIndex,
                                    maskParam = scope.dirty,
                                    hasNonStaticDefault = if (param is IrValueParameter) {
                                        param.defaultValue?.expression?.isStatic() == false
                                    } else {
                                        // No default for this parameter
                                        false
                                    }
                                )
                            }
                        }
                        return null
                    } else {
                        // If the capture is outside inline lambda, we don't allow meta propagation
                        if (!inlineLambdaInfo.isInlineLambda(scope.function) || inlineLambdaInfo.isCrossinlineLambda(scope.function)) {
                            return null
                        }
                    }
                }
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
        return null
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        return when (expression.origin) {
            IrStatementOrigin.FOR_LOOP -> {
                // The psi2ir phase will turn for loops into a block, so:
                //
                //   for (loopVar in <someIterable>)
                //
                // gets transformed into
                //
                //   // #1: The "header"
                //   val it = <someIterable>.iterator()
                //
                //   // #2: The inner while loop
                //   while (it.hasNext()) {
                //     val loopVar = it.next()
                //     // Loop body
                //   }
                //
                // Additionally, the IR lowering phase will take this block and optimize it
                // for some shapes of for loops. What we want to do is keep this original
                // shape in tact so that we don't ruin some of these optimizations.
                val statements = expression.statements

                require(statements.size == 2) {
                    "Expected 2 statements in for-loop block"
                }
                val oldVar = statements[0] as IrVariable
                require(oldVar.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR) {
                    "Expected FOR_LOOP_ITERATOR origin for iterator variable"
                }
                val newVar = oldVar.transform(this, null) as IrVariable

                val oldLoop = statements[1] as IrWhileLoop
                require(oldLoop.origin == IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
                    "Expected FOR_LOOP_INNER_WHILE origin for while loop"
                }

                val newLoop = oldLoop.transform(this, null)

                if (newVar == oldVar && newLoop == oldLoop)
                    expression
                else if (newLoop is IrBlock) {
                    require(newLoop.statements.size == 3)
                    val before = newLoop.statements[0] as IrContainerExpression
                    val loop = newLoop.statements[1] as IrWhileLoop
                    val after = newLoop.statements[2] as IrContainerExpression

                    val result = mutableStatementContainer()
                    result.statements.addAll(
                        listOf(
                            before,
                            irBlock(
                                type = expression.type,
                                origin = IrStatementOrigin.FOR_LOOP,
                                statements = listOf(
                                    newVar,
                                    loop
                                )
                            ),
                            after
                        )
                    )
                    result
                } else {
                    error("Expected transformed loop to be an IrBlock")
                }
            }
            IrStatementOrigin.FOR_LOOP_INNER_WHILE -> {
                val result = super.visitBlock(expression)
                result
            }
            else -> super.visitBlock(expression)
        }
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        if (expression.associatedComposableSingletonStub != null) {
            // This call has an associated stub in ComposableSingletons class. This stub is not
            // directly reachable by any code in this module, but might be used by other external libraries.
            // Transform it the same way as the one above.
            val getterCall = expression.associatedComposableSingletonStub
            val property = getterCall?.symbol?.owner?.correspondingPropertySymbol?.owner
            property?.transformChildrenVoid()
        }

        if (expression is IrCall && (expression.isComposableCall() || expression.isSyntheticComposableCall())) {
            return visitComposableCall(expression)
        }

        when {
            expression.symbol.owner.isInline || expression.symbol.owner.isInlineArrayConstructor() -> {
                val captureScope = Scope.CaptureScope()
                withScope(Scope.CallScope(expression, this)) {
                    expression.arguments.fastForEachIndexed { index, arg ->
                        val parameter = expression.symbol.owner.parameters[index]
                        val transformed = if (parameter.isInlineParameter()) {
                            // if it is not a composable call but it is an inline function, then we allow
                            // composable calls to happen inside of the inlined lambdas. This means that we have
                            // some control flow analysis to handle there as well. We wrap the call in a
                            // CaptureScope and coalescable group if the call has any composable invocations
                            // inside of it.
                            inScope(captureScope) { arg?.transform(this, null) }
                        } else {
                            arg?.transform(this, null)
                        }

                        expression.arguments[index] = transformed
                    }
                }
                return if (captureScope.hasCapturedComposableCall) {
                    captureScope.realizeAllDirectChildren()
                    expression.asCoalescableGroup(captureScope)
                } else {
                    expression
                }
            }
            expression is IrCall && expression.isComposableSingletonGetter() -> {
                // This looks like `ComposableSingletonClass.lambda-123`, which is a static/saved
                // call of composableLambdaInstance. We want to transform the property here now
                // so the assumptions about the invocation order assumed by source locations is
                // preserved.
                val getter = expression.symbol.owner
                val property = getter.correspondingPropertySymbol?.owner
                property?.transformChildrenVoid()
                return super.visitFunctionAccess(expression)
            }
            else -> return super.visitFunctionAccess(expression)
        }
    }

    private fun visitComposableCall(expression: IrCall): IrExpression {
        return when (expression.symbol.owner.kotlinFqName) {
            ComposeFqNames.remember -> {
                if (FeatureFlag.IntrinsicRemember.enabled) {
                    visitRememberCall(expression)
                } else {
                    visitNormalComposableCall(expression)
                }
            }
            ComposeFqNames.key -> visitKeyCall(expression)
            else -> visitNormalComposableCall(expression)
        }
    }

    private fun visitNormalComposableCall(expression: IrCall): IrExpression {
        val callScope = Scope.CallScope(expression, this)

        // it's important that we transform all of the parameters here since this will cause the
        // IrGetValue's of remapped default parameters to point to the right variable.
        inScope(callScope) {
            expression.transformChildrenVoid()
        }

        encounteredComposableCall(
            withGroups = !expression.symbol.owner.hasReadOnlyAnnotation,
        )

        val ownerFn = expression.symbol.owner
        val numParams = ownerFn.parameters.size
        val numDefaults: Int
        val numChanged: Int
        val numRealParams: Int

        val hasDefaults = ownerFn.parameters.any {
            it.kind == IrParameterKind.Regular && it.name == ComposeNames.DefaultParameter
        }
        if (!hasDefaults && expression.isInvoke()) {
            // in the case of an invoke without any defaults, all of the parameters are going to
            // be type parameter args which won't have special names.
            // In this case, we know that the values cannot
            // be defaulted though, so we can calculate the number of real parameters based on
            // the total number of parameters
            numDefaults = 0
            // TODO double check how this works with local lambdas since we add a slot for its instance
            //  on the function side
            numChanged = changedParamCountFromTotal(numParams)
            numRealParams = numParams -
                    1 - // composer param
                    numChanged
        } else {
            val composerParamIndex = ownerFn.parameters.indexOfFirst {
                it.kind == IrParameterKind.Regular && it.name == ComposeNames.ComposerParameter
            }
            require(composerParamIndex >= 0) {
                $$"Expected a $composer parameter in $${ownerFn.render()}"
            }
            numChanged = changedParamCount(composerParamIndex, 0 /* this params are already counted */)
            numDefaults = if (hasDefaults) {
                val receiverCount = ownerFn.thisParamCount
                defaultParamCount(composerParamIndex - receiverCount)
            } else {
                0
            }
            numRealParams = numParams -
                    1 - // composer param
                    numChanged -
                    numDefaults
        }

        val expectedNumParams = numRealParams +
                1 + // composer param
                numChanged +
                numDefaults
        require(numParams == expectedNumParams) {
            "Expected $expectedNumParams params for ${ownerFn.render()}, but got $numParams"
        }

        val composerIndex = numRealParams
        val changedArgIndex = composerIndex + 1
        val defaultArgIndex = changedArgIndex + numChanged
        val defaultArgs = (defaultArgIndex until numParams).map {
            expression.arguments[it]
        }
        val hasDefaultArgs = defaultArgs.isNotEmpty()

        val defaultMasks = defaultArgs.map {
            when (it) {
                !is IrConst -> error("Expected default mask to be a const")
                else -> it.value as? Int ?: error("Expected default mask to be an Int")
            }
        }

        var dispatchMeta: CallArgumentMeta? = null
        val argsMeta = mutableListOf<CallArgumentMeta>()
        var valueParamIndex = 0
        for (i in 0 until composerIndex) {
            val arg = expression.arguments[i]
            val param = ownerFn.parameters[i]
            if (arg == null) {
                if (param.varargElementType == null) {
                    // ComposerParamTransformer should not allow for any null arguments on a composable
                    // invocation unless the parameter is vararg. If this is null here, we have
                    // missed something.
                    error("Unexpected null argument for composable call")
                } else {
                    argsMeta.add(CallArgumentMeta(isVararg = true))
                    continue
                }
            }
            when (param.kind) {
                IrParameterKind.DispatchReceiver -> {
                    dispatchMeta = argumentMetaOf(arg, isProvided = true)
                }
                IrParameterKind.Context,
                IrParameterKind.ExtensionReceiver -> {
                    val meta = argumentMetaOf(arg, isProvided = true)
                    argsMeta.add(meta)
                }
                IrParameterKind.Regular -> {
                    val index = valueParamIndex++
                    val bitIndex = defaultsBitIndex(index)
                    val maskValue = if (hasDefaultArgs) defaultMasks[defaultsParamIndex(index)] else 0
                    val meta = argumentMetaOf(arg, isProvided = maskValue and (0b1 shl bitIndex) == 0)
                    argsMeta.add(meta)
                }
            }
        }

        val changedArgs = buildChangedArgumentsForCall(argsMeta + listOfNotNull(dispatchMeta))

        changedArgs.fastForEachIndexed { i, arg ->
            expression.arguments[changedArgIndex + i] = arg
        }

        currentFunctionScope.metrics.recordComposableCall(
            expression,
            argsMeta
        )
        metrics.recordComposableCall(
            expression,
            argsMeta
        )
        recordCallInSource(call = expression)

        return callScope.marker?.let {
            expression.variablePrefix(it)
        } ?: expression
    }

    private fun visitRememberCall(expression: IrCall): IrExpression {
        val inputArgs = mutableListOf<IrExpression>()
        var hasSpreadArgs = false
        var calculationArg: IrExpression? = null
        for (i in 0 until expression.arguments.size) {
            val param = expression.symbol.owner.parameters[i]
            val arg = expression.arguments[i]
                ?: error("Unexpected null argument found on key call")
            if (param.name.asString().startsWith('$')) {
                // we are done. synthetic args go at
                // the end
                break
            }

            when {
                param.name.identifier == "calculation" -> {
                    calculationArg = arg
                }

                arg is IrVararg -> {
                    inputArgs.addAll(
                        arg.elements.mapNotNull {
                            if (it is IrSpreadElement) {
                                hasSpreadArgs = true
                                arg
                            } else {
                                it as? IrExpression
                            }
                        }
                    )
                }

                else -> {
                    inputArgs.add(arg)
                }
            }
        }

        for (i in inputArgs.indices) {
            inputArgs[i] = inputArgs[i].transform(this, null)
        }

        encounteredComposableCall(withGroups = true)
        recordCallInSource(call = expression)

        if (calculationArg == null) {
            return expression
        }
        if (hasSpreadArgs) {
            calculationArg.transform(this, null)
            return expression
        }

        // Build the change parameters as if this was a call to remember to ensure the
        // use of the $dirty flags are calculated correctly.
        val inputArgMetas = inputArgs.map { argumentMetaOf(it, isProvided = true) }.also {
            buildChangedArgumentsForCall(it)
        }

        // If intrinsic remember uses $dirty, we are not sure if it is going to be populated,
        // so we have to apply fixups after function body is transformed
        var dirty: IrChangedBitMaskValue? = null
        inputArgMetas.fastForEach {
            val meta = it.paramRef
            if (meta?.maskParam is IrChangedBitMaskVariable) {
                if (dirty == null) {
                    dirty = meta.maskParam
                } else {
                    // Validate that we only capture dirty param from a single scope. Capturing
                    // $dirty is only allowed in inline functions, so we are guaranteed to only
                    // encounter one.
                    require(dirty == meta.maskParam) {
                        "Only single dirty param is allowed in a capture scope"
                    }
                }
            }
        }
        val usesDirty = inputArgMetas.any { it.paramRef?.maskParam is IrChangedBitMaskVariable }

        val isMemoizedLambda = expression.origin == ComposeMemoizedLambdaOrigin

        // We can only rely on the $changed or $dirty if the flags are correctly updated in
        // the restart function or the result of replacing remember with cached will be
        // different.
        val metaMaskConsistent = updateChangedFlagsFunction != null
        val changedFunction: (Boolean, IrExpression, CallArgumentMeta) -> IrExpression? =
            if (usesDirty || !metaMaskConsistent) {
                { _, arg, _ ->
                    irChanged(
                        arg,
                        compareInstanceForFunctionTypes = false,
                        compareInstanceForUnstableValues = isMemoizedLambda
                    )
                }
            } else {
                ::irIntrinsicChanged
            }

        // Hoist execution of input params outside of the remember group, similar to how it is
        // handled with inlining.
        val inputVals = inputArgs.mapIndexed { index, expr ->
            val meta = inputArgMetas[index]

            // Only create variables when reads introduce side effects
            val trivialExpression = meta.isCertain || expr is IrGetValue || expr is IrConst
            if (!trivialExpression) {
                irTemporary(expr, nameHint = "remember\$arg\$$index")
            } else {
                null
            }
        }
        val inputExprs = inputVals.mapIndexed { index, variable ->
            variable?.let { irGet(it) } ?: inputArgs[index]
        }
        val invalidExpr = irIntrinsicRememberInvalid(
            isMemoizedLambda,
            inputExprs,
            inputArgMetas,
            changedFunction
        )
        val functionScope = currentFunctionScope
        val cacheCall = irCache(
            irCurrentComposer(),
            expression.startOffset,
            expression.endOffset,
            expression.type,
            invalidExpr,
            calculationArg.transform(this, null)
        )
        if (usesDirty && metaMaskConsistent) {
            functionScope.recordIntrinsicRememberFixUp(
                isMemoizedLambda,
                inputExprs,
                inputArgMetas,
                cacheCall
            )
        }

        val blockScope = intrinsicRememberScope(expression)
        return inScope(blockScope) {
            val nonNullInputValues = inputVals.filterNotNull()
            if (useNonSkippingGroupOptimization) {
                val body = irWithSourceInformationMarker(
                    before = nonNullInputValues,
                    expression = cacheCall,
                    scope = blockScope,
                )
                // Ensure that the body of intrinsic remember is always represented as a block,
                // so that intrinsic remember propagates isStatic if needed.
                if (body !is IrBlock) {
                    body.wrap(type = body.type)
                } else {
                    body
                }
            } else {
                cacheCall.wrap(
                    before = nonNullInputValues + listOf(
                        irStartReplaceGroup(expression, blockScope, irFunctionSourceKey(expression.symbol.owner))
                    ),
                    after = listOf(irEndReplaceGroup(scope = blockScope))
                )
            }
        }.also { expr ->
            if (
                stabilityInferencer.stabilityOf(expr.type).knownStable() &&
                inputArgMetas.all { it.isStatic }
            ) {
                context.irTrace.record(ComposeWritableSlices.IS_STATIC_EXPRESSION, expr, true)
            }
        }
    }

    private fun intrinsicRememberScope(
        rememberCall: IrCall,
    ) = object : Scope.BlockScope("<intrinsic-remember>") {
        val rememberFunction = rememberCall.symbol.owner
        val currentFunction = currentFunctionScope.function
        override fun calculateHasSourceInformation(sourceInformationEnabled: Boolean) =
            sourceInformationEnabled

        override fun calculateSourceInfo(sourceInformationEnabled: Boolean): String? =
        // forge a source information call to fake remember function with current file
            // location to make sure tooling can identify the following group as remember.
            if (sourceInformationEnabled) {
                buildString {
                    append(rememberFunction.callInformation())
                    super.calculateSourceInfo(true)?.also {
                        append(it)
                    }
                    append(":")
                    append(currentFunction.file.name)
                    append("#")
                    // Use runtime package hash to make sure tooling can identify it as such
                    append(rememberFunction.packageHash().toString(36))
                }
            } else {
                null
            }
    }

    private fun irIntrinsicRememberInvalid(
        isMemoizedLambda: Boolean,
        args: List<IrExpression>,
        metas: List<CallArgumentMeta>,
        changedExpr: (Boolean, IrExpression, CallArgumentMeta) -> IrExpression?,
    ): IrExpression =
        args
            .mapIndexedNotNull { i, arg -> changedExpr(isMemoizedLambda, arg, metas[i]) }
            .reduceOrNull { acc, changed -> irBooleanOr(acc, changed) }
            ?: irConst(false)

    private fun irIntrinsicChanged(
        isMemoizedLambda: Boolean,
        arg: IrExpression,
        argInfo: CallArgumentMeta,
    ): IrExpression? {
        val meta = argInfo.paramRef
        val param = meta?.maskParam
        return when {
            argInfo.isStatic -> null
            argInfo.isCertain &&
                    argInfo.stability.knownStable() &&
                    param is IrChangedBitMaskVariable &&
                    !meta.hasNonStaticDefault -> {
                // if it's a dirty flag, and the parameter doesn't have a default value and is _known_
                // to be stable, then we know that the value is now CERTAIN, thus we can avoid
                // calling changed completely
                //
                // invalid = invalid or (mask == different)
                irEqual(
                    param.irIsolateBitsAtSlot(meta.maskSlot, includeStableBit = true),
                    irConst(ParamState.Different.bitsForSlot(meta.maskSlot))
                )
            }
            argInfo.isCertain &&
                    !argInfo.stability.knownUnstable() &&
                    param is IrChangedBitMaskVariable &&
                    !meta.hasNonStaticDefault -> {
                // if it's a dirty flag, and the parameter doesn't have a default value and it might
                // be stable, then we only check changed if the value is unstable, otherwise we can
                // just check to see if the mask is different
                //
                // invalid = invalid or (stable && mask == different || unstable && changed)

                val maskIsStableAndDifferent = irEqual(
                    param.irIsolateBitsAtSlot(meta.maskSlot, includeStableBit = true),
                    irConst(ParamState.Different.bitsForSlot(meta.maskSlot))
                )
                val stableBits = param.irSlotAnd(meta.maskSlot, StabilityBits.UNSTABLE.bits)
                val maskIsUnstableAndChanged = irAndAnd(
                    irNotEqual(stableBits, irConst(0)),
                    irChanged(
                        arg,
                        compareInstanceForFunctionTypes = false,
                        compareInstanceForUnstableValues = isMemoizedLambda
                    )
                )
                irOrOr(
                    maskIsStableAndDifferent,
                    maskIsUnstableAndChanged
                )
            }
            argInfo.isCertain &&
                    !argInfo.stability.knownUnstable() &&
                    param != null -> {
                // if it's a changed flag or parameter with a default expression then uncertain is a
                // possible value. If  it is uncertain OR unstable, then we need to call changed.
                // If it is uncertain or unstable here it will _always_ be uncertain or unstable
                // here, so this is safe. If it is not uncertain or unstable, we can just check to
                // see if its different

                //     unstableOrUncertain = mask xor 011 > 010
                //     invalid = invalid or ((unstableOrUncertain && changed()) || mask == different)

                val maskIsUnstableOrUncertain =
                    irGreater(
                        irXor(
                            param.irIsolateBitsAtSlot(meta.maskSlot, includeStableBit = true),
                            irConst(bitsForSlot(0b011, meta.maskSlot))
                        ),
                        irConst(bitsForSlot(0b010, meta.maskSlot))
                    )
                irOrOr(
                    irAndAnd(
                        maskIsUnstableOrUncertain,
                        irChanged(
                            arg,
                            compareInstanceForFunctionTypes = false,
                            compareInstanceForUnstableValues = isMemoizedLambda
                        )
                    ),
                    irEqual(
                        param.irIsolateBitsAtSlot(meta.maskSlot, includeStableBit = false),
                        irConst(ParamState.Different.bitsForSlot(meta.maskSlot))
                    )
                )
            }
            else -> irChanged(
                arg,
                compareInstanceForFunctionTypes = false,
                compareInstanceForUnstableValues = isMemoizedLambda
            )
        }
    }

    private fun visitKeyCall(expression: IrCall): IrExpression {
        encounteredComposableCall(withGroups = true)
        val keyArgs = mutableListOf<IrExpression>()
        var blockArg: IrExpression? = null
        for (i in 0 until expression.arguments.size) {
            val param = expression.symbol.owner.parameters[i]
            val arg = expression.arguments[i]
                ?: error("Unexpected null argument found on key call")
            if (param.name.asString().startsWith('$')) {
                // we are done. synthetic args go at
                // the end
                break
            }

            when {
                param.name.identifier == "block" -> {
                    blockArg = arg
                }
                arg is IrVararg -> {
                    keyArgs.addAll(arg.elements.mapNotNull { it as? IrExpression })
                }
                else -> {
                    keyArgs.add(arg)
                }
            }
        }
        val before = mutableStatementContainer()
        val after = mutableStatementContainer()

        if (blockArg !is IrFunctionExpression)
            error("Expected function expression but was ${blockArg?.let { it::class }}")

        val (block, resultVar) = blockArg.function.body!!.asBodyAndResultVar(expectedTarget = blockArg.function)

        var transformed: IrExpression = block

        val scope = withScope(Scope.KeyScope()) {
            transformed = transformed.transform(this, null)
        }

        // now after the inner block is extracted, the $composer parameter used in the block needs
        // to be remapped to the outer composer instead for the expression and any inlined lambdas.
        block.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement =
                if (inlineLambdaInfo.isInlineLambda(declaration)) {
                    super.visitFunction(declaration)
                } else {
                    declaration
                }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                super.visitGetValue(expression)

                val value = expression.symbol.owner
                return if (
                    value is IrValueParameter && value.name == ComposeNames.ComposerParameter
                ) {
                    irCurrentComposer()
                } else {
                    expression
                }
            }
        })

        // Key should behave the same way as any other inlined lambda, so we need to
        // realize all inner control flow if composable calls are found inside of it.
        if (scope.hasComposableCalls) {
            scope.realizeAllDirectChildren()
            // Realize groups here since `key` itself is not coalescable.
            scope.realizeCoalescableGroup()
        }
        scope.realizeEndCalls {
            irEndMovableGroup(scope)
        }

        return irBlock(
            type = expression.type,
            statements = listOfNotNull(
                before,
                irStartMovableGroup(
                    expression,
                    irJoinKeyChain(keyArgs.map { it.transform(this, null) }),
                    scope
                ),
                block,
                irEndMovableGroup(scope),
                after,
                resultVar?.let { irGet(resultVar) }
            )
        )
    }

    private fun buildChangedArgumentsForCall(args: List<CallArgumentMeta>): List<IrExpression> {
        // passing in 0 for thisParams since they should be included in the params list
        val changedCount = changedParamCount(args.size, 0)
        val result = mutableListOf<IrExpression>()
        for (i in 0 until changedCount) {
            val start = i * SLOTS_PER_INT
            val end = min(start + SLOTS_PER_INT, args.size)
            val slice = args.subList(start, end)
            result.add(buildChangedArgumentForCall(slice))
        }
        return result
    }

    private fun buildChangedArgumentForCall(arguments: List<CallArgumentMeta>): IrExpression {
        // The general pattern here is:
        //
        // $changed = bitMaskConstant or
        // (0b11 and someMask shl y) or
        // (0b1100 and someMask shl x) or
        // ...
        // (0b11000000 and someMask shr z)
        //
        // where `bitMaskConstant` is created in this function based on
        // all of the static (constant) params and uncertain params (not direct parameter pass
        // throughs). The other params have had their state made "certain" by the preamble checks
        // in a composable function in scope. We can extract that state directly by pulling out
        // the specific slot state from that function's dirty parameter (represented as
        // `someMask` here, and then shifting the resulting bit mask over to the correct slot
        // (the shift amount represented here by `x`, `y`, and `z`).

        // TODO: we could make some small optimization here if we have multiple values passed
        //  from one function into another in the same order. This may not happen commonly enough
        //  to be worth the complication though.

        // NOTE: we start with 0b0 because it is important that the low bit is always 0
        var bitMaskConstant = 0b0
        val orExprs = mutableListOf<IrExpression>()

        arguments.fastForEachIndexed { slot, argInfo ->
            val stability = argInfo.stability
            when {
                !FeatureFlag.StrongSkipping.enabled && stability.knownUnstable() -> {
                    bitMaskConstant = bitMaskConstant or StabilityBits.UNSTABLE.bitsForSlot(slot)
                    // If it is known to be unstable, there's no purpose in propagating any
                    // additional metadata _for this parameter_, but we still want to propagate
                    // the other parameters.
                    return@fastForEachIndexed
                }
                stability.knownStable() -> {
                    bitMaskConstant = bitMaskConstant or StabilityBits.STABLE.bitsForSlot(slot)
                }
                else -> {
                    stability.irStableExpression(
                        resolve = {
                            irTypeParameterStability(it)
                        }
                    )?.let {
                        val expr = if (slot == 0) {
                            it
                        } else {
                            val bitsToShiftLeft = slot * BITS_PER_SLOT
                            irShl(it, irConst(bitsToShiftLeft))
                        }
                        orExprs.add(expr)
                    }
                }
            }
            if (argInfo.isVararg) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else if (!argInfo.isProvided) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else if (argInfo.isStatic) {
                bitMaskConstant = bitMaskConstant or ParamState.Static.bitsForSlot(slot)
            } else if (!argInfo.isCertain) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else {
                val meta = argInfo.paramRef ?: error("Meta is required if param is Certain")
                val someMask = meta.maskParam ?: error("Mask param required if param is Certain")
                val parentSlot = meta.maskSlot
                require(parentSlot != -1) { "invalid parent slot for Certain param" }

                // if parentSlot is lower than slot, we shift left a positive amount of bits
                orExprs.add(
                    irAnd(
                        irConst(ParamState.Mask.bitsForSlot(slot)),
                        someMask.irShiftBits(parentSlot, slot)
                    )
                )
            }
        }
        return when {
            // if there are no orExprs, then we can just use the constant
            orExprs.isEmpty() -> irConst(bitMaskConstant)
            // if the constant is still 0, then we can just use the or expressions. This is safe
            // because the low bit will still be 0 regardless of the result of the or expressions.
            bitMaskConstant == 0 -> orExprs.reduce { lhs, rhs ->
                irOr(lhs, rhs)
            }
            // otherwise, we do (bitMaskConstant or a or b ... or z)
            else -> orExprs.fold<IrExpression, IrExpression>(irConst(bitMaskConstant)) { lhs, rhs ->
                irOr(lhs, rhs)
            }
        }
    }

    private fun irTypeParameterStability(param: IrTypeParameter): IrExpression? {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.isComposable) {
                        val fn = scope.function
                        val maskParam = scope.dirty ?: scope.changedParameter
                        if (maskParam != null && fn.typeParameters.isNotEmpty()) {
                            for (it in fn.parameters) {
                                val classifier = it.type.classifierOrNull
                                if (classifier == param.symbol) {
                                    val parentSlot = scope.allTrackedParams.indexOf(it)
                                    if (parentSlot == -1) return null
                                    return irAnd(
                                        irConst(StabilityBits.UNSTABLE.bitsForSlot(0)),
                                        maskParam.irShiftBits(parentSlot, 0)
                                    )
                                }
                            }
                        }
                    }
                }
                is Scope.RootScope,
                is Scope.FileScope,
                is Scope.ClassScope,
                -> {
                    break@loop
                }
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
        return null
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val declaration = expression.symbol.owner
        var scope: Scope? = currentScope
        if (declaration is IrValueParameter) {
            val fn = declaration.parent
            while (scope != null) {
                if (scope is Scope.FunctionScope) {
                    if (scope.function == fn) {
                        val index = scope.allTrackedParams.indexOf(declaration)
                        if (index != -1) {
                            scope.usedParams[index] = true
                        }
                        return expression
                    }
                }
                scope = scope.parent
            }
        }
        return expression
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (!isInComposableScope || currentFunctionScope.preserveIrShape) return super.visitReturn(expression)
        val scope = Scope.ReturnScope(expression)
        withScope(scope) {
            expression.transformChildrenVoid()
        }
        val endBlock = mutableStatementContainer()
        encounteredReturn(expression.returnTargetSymbol) { endBlock.statements.add(it) }
        return if (
            !scope.hasComposableCalls && expression.value.type.isUnitOrNullableUnit()
        ) {
            expression.wrap(listOf(endBlock))
        } else {
            val tempVar = irTemporary(expression.value, nameHint = "return")
            tempVar.wrap(
                expression.startOffset,
                expression.endOffset,
                expression.type,
                after = listOf(
                    endBlock,
                    IrReturnImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        expression.returnTargetSymbol,
                        irGet(tempVar)
                    )
                )
            )
        }
    }

    override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
        if (!isInComposableScope) return super.visitBreakContinue(jump)
        val endBlock = mutableStatementContainer()
        encounteredJump(jump) { endBlock.statements.add(it) }
        return jump.wrap(before = listOf(endBlock))
    }

    override fun visitDoWhileLoop(loop: IrDoWhileLoop): IrExpression {
        if (!isInComposableScope) return super.visitDoWhileLoop(loop)
        return handleLoop(loop)
    }

    override fun visitWhileLoop(loop: IrWhileLoop): IrExpression {
        if (!isInComposableScope) return super.visitWhileLoop(loop)
        return handleLoop(loop)
    }

    private fun handleLoop(loop: IrLoop): IrExpression {
        val loopScope = Scope.LoopScope(loop)
        withScope(loopScope) {
            loop.condition = loop.condition.transform(this, null)
            if (loopScope.needsGroupPerIteration && loopScope.hasComposableCalls) {
                loop.condition = loop.condition.asReplaceGroup(loopScope)
            }

            loop.body = loop.body?.transform(this, null)
            if (loopScope.needsGroupPerIteration && loopScope.hasComposableCalls) {
                val current = loop.body
                if (current is IrBlock) {
                    /*
                     * Kotlin optimizes for loops by separating them into three pieces
                     *   #1: The "header"
                     *   val it = <someIterable>.iterator()
                     *
                     *   #2: The condition
                     *   while (it.hasNext()) {
                     *       val loopVar = it.next()
                     *       #3: The loop body
                     *   }
                     *
                     * We need to generate groups inside the "body", otherwise the behavior is
                     * undefined, so we find the loopVar and insert groups after it.
                     */
                    val forLoopVariableIndex = current.statements.indexOfFirst {
                        (it as? IrVariable)?.origin == IrDeclarationOrigin.FOR_LOOP_VARIABLE
                    }
                    loop.body = current.withReplaceGroupStatements(
                        loopScope,
                        insertAt = forLoopVariableIndex + 1
                    )
                } else {
                    loop.body = current?.asReplaceGroup(loopScope)
                }
            }
        }
        return if ((!loopScope.needsGroupPerIteration || (
                    !currentFunctionScope.outerGroupRequired &&
                            // if we end up getting an early return this group will come back
                            // However this might generate less efficient (but still correct code) if the
                            // early return is encountered after the loop.
                            !currentFunctionScope.hasAnyEarlyReturn)
                    ) && loopScope.hasComposableCalls
        ) {
            // If a loop contains composable calls but not a otherwise need a group per iteration
            // group, none of the children can be coalesced and must be realized as the second
            // iteration as composable calls at the end might end of overlapping slots with the
            // start of the loop. See b/232007227 for details.
            loopScope.realizeAllDirectChildren()
            loop.asCoalescableGroup(loopScope)
        } else {
            loop
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        if (!isInComposableScope) return super.visitWhen(expression)
        if (currentFunctionScope.function.hasExplicitGroups) return super.visitWhen(expression)

        val optimizeGroups = FeatureFlag.OptimizeNonSkippingGroups.enabled

        // Composable calls in conditions are more expensive than composable calls in the different
        // result branches of the when clause. This is because if we have N branches of a when
        // clause, we will always execute exactly 1 result branch, but we will execute 0-N of the
        // conditions. This means that if only the results have composable calls, we can use
        // replace groups to represent the entire expression. If a condition has a composable
        // call in it, we need to place the whole expression in a Container group, since a variable
        // number of them will be created. The exception here is the first branch's condition,
        // since it will *always* be executed. As a result, if only the first conditional has a
        // composable call in it, we can avoid creating a group for it since it is not
        // conditionally executed.
        var needsWrappingGroup = false
        var resultsWithCalls = 0
        var hasElseBranch = false

        val transformed = IrWhenImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            expression.origin
        )
        val resultScopes = mutableListOf<Scope.BranchScope>()
        val condScopes = mutableListOf<Scope.BranchScope>()
        val whenScope = withScope(Scope.WhenScope()) {
            expression.branches.fastForEachIndexed { index, it ->
                if (it is IrElseBranch) {
                    hasElseBranch = true
                    val (resultScope, result) = it.result.transformWithScope(Scope.BranchScope())

                    condScopes.add(Scope.BranchScope())
                    resultScopes.add(resultScope)

                    if (resultScope.hasComposableCalls)
                        resultsWithCalls++

                    transformed.branches.add(
                        IrElseBranchImpl(
                            it.startOffset,
                            it.endOffset,
                            it.condition,
                            result
                        )
                    )
                } else {
                    val (condScope, condition) = it
                        .condition
                        .transformWithScope(Scope.BranchScope())
                    val (resultScope, result) = it
                        .result
                        .transformWithScope(Scope.BranchScope())

                    condScopes.add(condScope)
                    resultScopes.add(resultScope)

                    // the first condition is always executed so if it has a composable call in it,
                    // it doesn't necessitate a group. However, non-skipping group optimization is
                    // enabled, we need a wrapping group if any conditions have a composable call.
                    needsWrappingGroup = needsWrappingGroup || ((index != 0) && condScope.hasComposableCalls)

                    if (resultScope.hasComposableCalls && !it.result.isGroupBalanced())
                        resultsWithCalls++

                    transformed.branches.add(
                        IrBranchImpl(
                            it.startOffset,
                            it.endOffset,
                            condition,
                            result
                        )
                    )
                }
            }
        }

        // If we are optimizing the non-skipping functions we always need the
        // same number of groups if any of the results have composable functions
        // and it needs to be the same number even if only one branch requires a
        // group.
        val needsResultGroups = if (optimizeGroups) {
            resultsWithCalls > 0
        } else {
            resultsWithCalls > 1 && !needsWrappingGroup
        }

        // If we are putting groups around the result branches, we need to guarantee that exactly
        // one result branch is executed. We do this by adding an else branch if it there is not
        // one already. Note that we only need to do this if we aren't going to wrap the if
        // statement in a group entirely, which we will do if the conditions have calls in them.
        // NOTE: we might also be able to assume that the when is exhaustive if it has a non-unit
        // resulting type, since the type system should enforce that.
        if (!hasElseBranch && needsResultGroups) {
            condScopes.add(Scope.BranchScope())
            resultScopes.add(Scope.BranchScope())
            transformed.branches.add(
                irElseBranch(
                    expression = irBlock(
                        type = context.irBuiltIns.unitType,
                        statements = emptyList()
                    )
                )
            )
        }

        forEachWith(transformed.branches, condScopes, resultScopes) { it, condScope, resultScope ->
            if (condScope.hasComposableCalls) {
                if (needsWrappingGroup && !optimizeGroups) {
                    // Generate a group around the conditional block when it has a composable call
                    // in it and we are generating a group around when block.
                    it.condition = it.condition.asReplaceGroup(condScope)
                } else {
                    // Ensure that the inner structure of condition is correct if the wrapping group
                    // is not required by realizing groups in condition scope.
                    condScope.realizeAllDirectChildren()
                    condScope.realizeCoalescableGroup()
                }
            }

            // if no wrapping group but more than we need branch groups, we have to have every
            // result be a group so that we have a consistent number of groups during execution
            if (
                needsResultGroups ||
                // if we are wrapping the if with a group, then we only need to add a group when
                // the block has composable calls. The check of the feature flag check here is redundant
                // as needsBranchGroups will be true if any result scope has composable calls but it
                // is here redundantly so when this flag is removed this code will be updated.
                !optimizeGroups && (needsWrappingGroup && resultScope.hasComposableCalls)
            ) {
                it.result = it.result.asReplaceGroup(resultScope)
            }

            if (resultsWithCalls == 1 && resultScope.hasComposableCalls) {
                // Realize all groups in the branch result with a conditional call - making sure
                // that nested control structures are wrapped correctly.
                resultScope.realizeCoalescableGroup()
            }
        }

        if (
            optimizeGroups && needsResultGroups && (
                    transformed.origin == IrStatementOrigin.ANDAND || transformed.origin == IrStatementOrigin.OROR
                    )
        ) {
            // When a IrWhen has a ANDAND or OROR origin it is required they also have a specific shape such as for ANDAND requires a
            // `true -> false` clause at the end.  As we violate this by adding a wrapping group around all results, this origin is removed
            // down-stream lowerings will no longer special case this IrWhen.
            transformed.origin = IrStatementOrigin.WHEN
        }

        return when {
            ((!optimizeGroups && resultsWithCalls == 1) || needsWrappingGroup) ->
                transformed.asCoalescableGroup(whenScope)
            else -> transformed
        }
    }

    // Returns true if the number of groups added are required to be fix and a group is inserted  to balance the groups if they are not.
    // Currently this is only guaranteed for IrWhen nodes when the group non-skipping group optimization is enabled. This avoids
    // inserting a redundant group to balance an already balanced set of groups.
    private fun IrExpression.isGroupBalanced(): Boolean = when (this) {
        is IrWhen -> FeatureFlag.OptimizeNonSkippingGroups.enabled
        else -> false
    }

    sealed class Scope(val name: String) {
        var parent: Scope? = null
        var level: Int = 0

        open val isInComposable get() = false
        open val functionScope: FunctionScope? get() = parent?.functionScope
        open val fileScope: FileScope? get() = parent?.fileScope
        open val nearestComposer: IrValueParameter? get() = parent?.nearestComposer

        val myComposer: IrValueParameter
            get() = nearestComposer
                ?: error("Not in a composable function")

        open class SourceLocation(val element: IrElement) {
            open val repeatable: Boolean
                get() = false
            var used = false
                private set

            fun markUsed() {
                used = true
            }
        }

        class RootScope : Scope("<root>")
        class FunctionScope(
            val function: IrFunction,
            private val transformer: ComposableFunctionBodyTransformer,
        ) : BlockScope("fun ${function.name.asString()}") {
            val isInlinedLambda: Boolean
                get() = transformer.inlineLambdaInfo.isInlineLambda(function)
            val isCrossinlineLambda: Boolean
                get() = transformer.inlineLambdaInfo.isCrossinlineLambda(function)

            val inComposableCall: Boolean
                get() = (parent as? CallScope)?.expression?.let { call ->
                    with(transformer) {
                        call is IrCall && (call.isComposableCall() || call.isSyntheticComposableCall())
                    }
                } == true

            val metrics: FunctionMetrics = transformer.metricsFor(function)

            var hasInlineEarlyReturn: Boolean = false
            var hasAnyEarlyReturn: Boolean = false

            private var lastTemporaryIndex: Int = 0

            private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

            override val isInComposable: Boolean
                get() = isComposable ||
                        transformer.inlineLambdaInfo.preservesComposableScope(function) &&
                        parent?.isInComposable == true

            override val functionScope: FunctionScope get() = this
            override val nearestComposer: IrValueParameter?
                get() = composerParameter ?: super.nearestComposer

            var composerParameter: IrValueParameter? = null
                private set

            var defaultParameter: IrDefaultBitMaskValue? = null
                private set

            var changedParameter: IrChangedBitMaskValue? = null
                private set

            var realValueParamCount: Int = 0
                private set

            // slotCount will include the dispatchReceiver, extensionReceivers and context receivers
            var slotCount: Int = 0
                private set

            var dirty: IrChangedBitMaskValue? = null

            var outerGroupRequired = false

            var preserveIrShape = false

            val markerPreamble = mutableStatementContainer(transformer.context)
            private var marker: IrVariable? = null

            fun allocateMarker(): IrVariable = marker ?: run {
                val parent = parent
                return when {
                    isInlinedLambda && !isComposable && parent is CallScope -> {
                        parent.allocateMarker()
                    }
                    else -> {
                        val newMarker = transformer.irTemporary(
                            transformer.irCurrentMarker(myComposer),
                            getNameForTemporary("marker")
                        )
                        markerPreamble.statements.add(newMarker)
                        marker = newMarker
                        newMarker
                    }
                }
            }

            private fun parameterInformation(): String =
                function.parameterInformation()

            override fun sourceLocationOf(call: IrElement): SourceLocation {
                val parent = parent
                return if (isInlinedLambda && parent is BlockScope)
                    parent.sourceLocationOf(call)
                else super.sourceLocationOf(call)
            }

            private fun callInformation(): String =
                function.callInformation()

            override fun calculateHasSourceInformation(sourceInformationEnabled: Boolean): Boolean {
                return if (sourceInformationEnabled) {
                    if (function.isLambda() && !isInlinedLambda)
                        super.calculateHasSourceInformation(sourceInformationEnabled)
                    else
                        true
                } else function.visibility.isPublicAPI
            }

            override fun calculateSourceInfo(sourceInformationEnabled: Boolean): String? =
                if (sourceInformationEnabled) {
                    "${callInformation()}${parameterInformation()}${
                        super.calculateSourceInfo(sourceInformationEnabled) ?: ""
                    }:${function.sourceFileInformation()}"
                } else {
                    if (function.visibility.isPublicAPI) {
                        "${callInformation()}${parameterInformation()}"
                    } else {
                        null
                    }
                }

            init {
                val defaultParams = mutableListOf<IrValueParameter>()
                val changedParams = mutableListOf<IrValueParameter>()
                for (param in function.parameters) {
                    when (param.kind) {
                        IrParameterKind.DispatchReceiver,
                        IrParameterKind.ExtensionReceiver,
                        IrParameterKind.Context -> {
                            slotCount++
                        }
                        IrParameterKind.Regular -> {
                            val paramName = param.name.asString()
                            when {
                                paramName == ComposeNames.ComposerParameter.identifier ->
                                    composerParameter = param
                                paramName.startsWith(ComposeNames.DefaultParameter.identifier) ->
                                    defaultParams += param
                                paramName.startsWith(ComposeNames.ChangedParameter.identifier) ->
                                    changedParams += param
                                paramName.startsWith("\$context_receiver_") ||
                                        paramName.startsWith("\$name\$for\$destructuring") ||
                                        paramName.startsWith("\$noName_") ||
                                        paramName == "\$this" -> Unit
                                else -> realValueParamCount++
                            }
                        }
                    }
                }
                slotCount += realValueParamCount
                if (function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
                    slotCount++
                }
                changedParameter = if (composerParameter != null) {
                    transformer.IrChangedBitMaskValueImpl(
                        changedParams,
                        slotCount
                    )
                } else {
                    null
                }
                defaultParameter = if (defaultParams.isNotEmpty()) {
                    transformer.IrDefaultBitMaskValueImpl(
                        defaultParams,
                        realValueParamCount,
                    )
                } else {
                    null
                }
            }

            val isComposable = composerParameter != null

            val allTrackedParams = buildList {
                var parameterCount = realValueParamCount
                // reorder to match $changed: [context, extension, value, dispatch]
                function.parameters.fastForEach {
                    if (it.kind == IrParameterKind.Context || it.kind == IrParameterKind.ExtensionReceiver) {
                        add(it)
                    }
                }
                function.parameters.fastForEach {
                    if (parameterCount > 0 && it.kind == IrParameterKind.Regular) {
                        parameterCount--
                        add(it)
                    }
                }
                function.parameters.fastForEach {
                    if (it.kind == IrParameterKind.DispatchReceiver) {
                        add(it)
                    }
                }
            }

            val valueArgsStart = allTrackedParams.indexOfFirst { it.kind == IrParameterKind.Regular }
            fun defaultIndexForSlotIndex(index: Int): Int {
                return index - valueArgsStart
            }

            val usedParams = BooleanArray(slotCount) { false }

            init {
                if (
                    isComposable &&
                    (
                            // We are interested in any object which has skippable function body and
                            // is being able to capture values from outside scope. Technically, that
                            // means we almost never skip in capture-less objects, but it is still more
                            // correct /not/ to skip when its dispatcher receiver changes. In most
                            // cases, we memoize these objects too (e.g fun interface) so the receiver
                            // should === with the previous instances most of time.
                            function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA ||
                                    function.dispatchReceiverParameter
                                        ?.type
                                        ?.classOrNull
                                        ?.owner
                                        ?.isLocal == true
                            )
                ) {
                    // in the case of a composable lambda/anonymous object, we want to make sure
                    // the dispatch receiver is always marked as "used"
                    usedParams[slotCount - 1] = true
                }
            }

            fun getNameForTemporary(nameHint: String?): String {
                val index = nextTemporaryIndex()
                return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
            }

            private class IntrinsicRememberFixup(
                val isMemoizedLambda: Boolean,
                val args: List<IrExpression>,
                val metas: List<CallArgumentMeta>,
                val call: IrCall,
            )

            private val intrinsicRememberFixups = mutableListOf<IntrinsicRememberFixup>()

            fun recordIntrinsicRememberFixUp(
                isMemoizedLambda: Boolean,
                args: List<IrExpression>,
                metas: List<CallArgumentMeta>,
                call: IrCall,
            ) {
                val dirty = metas.find { it.paramRef?.maskParam is IrChangedBitMaskVariable }
                if (dirty?.paramRef?.maskParam == this.dirty) {
                    intrinsicRememberFixups.add(
                        IntrinsicRememberFixup(isMemoizedLambda, args, metas, call)
                    )
                } else {
                    // capturing dirty is only allowed from inline function context, which doesn't
                    // have dirty params.
                    // if we encounter dirty that doesn't match mask from the current function, it
                    // means that we should apply the fixup higher in the tree.
                    var scope = parent
                    while (scope !is FunctionScope) scope = scope!!.parent
                    scope.recordIntrinsicRememberFixUp(isMemoizedLambda, args, metas, call)
                }
            }

            fun applyIntrinsicRememberFixups(
                invalidExpr: (
                    isMemoizedLambda: Boolean,
                    List<IrExpression>,
                    List<CallArgumentMeta>,
                ) -> IrExpression,
            ) {
                intrinsicRememberFixups.fastForEach {
                    val invalid = invalidExpr(it.isMemoizedLambda, it.args, it.metas)
                    // $composer.cache(invalid, calc)
                    // 0th argument is the $composer
                    it.call.arguments[1] = invalid
                }
            }
        }

        abstract class BlockScope(name: String) : Scope(name) {
            private val extraEndLocations = mutableListOf<(IrExpression) -> Unit>()
            private val sourceLocations = mutableListOf<SourceLocation>()

            override val isInComposable: Boolean get() = parent?.isInComposable ?: false

            fun realizeGroup(makeEnd: (() -> IrExpression)?) {
                realizeCoalescableGroup()
                makeEnd?.let { realizeEndCalls(it) }
            }

            fun recordComposableCall(withGroups: Boolean) {
                hasComposableCalls = true
                if (withGroups) {
                    hasComposableCallsWithGroups = true
                }
                if (coalescableChildren.isNotEmpty()) {
                    // if a call happens after the coalescable child group, then we should
                    // realize the group of the coalescable child
                    coalescableChildren.last().shouldRealize = true
                }
            }

            fun realizeAllDirectChildren() {
                if (coalescableChildren.isNotEmpty()) {
                    coalescableChildren.fastForEach {
                        it.shouldRealize = true
                    }
                }
            }

            fun recordSourceLocation(call: IrElement, location: SourceLocation?): SourceLocation {
                return (location ?: sourceLocationOf(call)).also { sourceLocations.add(it) }
            }

            fun markReturn(extraEndLocation: (IrExpression) -> Unit) {
                hasReturn = true
                extraEndLocations.push(extraEndLocation)
            }

            fun markJump(extraEndLocation: (IrExpression) -> Unit) {
                hasJump = true
                extraEndLocations.push(extraEndLocation)
            }

            fun markCoalescableGroup(
                scope: BlockScope,
                realizeGroup: () -> Unit,
                makeEnd: () -> IrExpression,
            ) {
                addProvisionalSourceLocations(scope.sourceLocations)
                val groupInfo = CoalescableGroupInfo(
                    scope,
                    realizeGroup,
                    makeEnd
                )
                coalescableChildren.add(groupInfo)
            }

            open fun calculateHasSourceInformation(sourceInformationEnabled: Boolean): Boolean =
                sourceInformationEnabled && sourceLocations.isNotEmpty()

            open fun calculateSourceInfo(sourceInformationEnabled: Boolean): String? {
                return if (sourceInformationEnabled && sourceLocations.isNotEmpty()) {
                    val locations = sourceLocations
                        .filter {
                            !it.used &&
                                    it.element.startOffset != UNDEFINED_OFFSET &&
                                    it.element.endOffset != UNDEFINED_OFFSET
                        }
                        .distinct()
                    var markedRepeatable = false
                    val fileEntry = fileScope?.declaration?.fileEntry
                    if (locations.isEmpty()) null
                    else locations.joinToString(",") {
                        it.markUsed()
                        val lineNumber = fileEntry?.getLineNumber(it.element.startOffset) ?: ""
                        val offset = if (it.element.startOffset < it.element.endOffset) {
                            "@${it.element.startOffset}L${
                                it.element.endOffset - it.element.startOffset
                            }"
                        } else "@${it.element.startOffset}"
                        if (it.repeatable && !markedRepeatable) {
                            markedRepeatable = true
                            "*$lineNumber$offset"
                        } else {
                            "$lineNumber$offset"
                        }
                    }
                } else null
            }

            open fun sourceLocationOf(call: IrElement) = SourceLocation(call)

            // Add source locations that might be out of order as well as might be
            // used before they are realized into `sourceInformation()`. This is used
            // by coalesable groups which will mark their source locations used if they
            // become realized.
            fun addProvisionalSourceLocations(locations: List<SourceLocation>) {
                sourceLocations += locations
            }

            fun realizeCoalescableGroup() {
                coalescableChildren.fastForEach {
                    it.realize()
                }
            }

            open fun realizeEndCalls(makeEnd: () -> IrExpression) {
                extraEndLocations.fastForEach {
                    it(makeEnd())
                }
            }

            var hasDefaultsGroup = false
            var hasComposableCallsWithGroups = false
                private set
            var hasComposableCalls = false
                private set
            var hasReturn = false
                private set
            var hasJump = false
                protected set
            private val coalescableChildren = mutableListOf<CoalescableGroupInfo>()

            class CoalescableGroupInfo(
                private val scope: BlockScope,
                private val realizeGroup: () -> Unit,
                private val makeEnd: () -> IrExpression,
            ) {
                var shouldRealize = false
                private var realized = false
                fun realize() {
                    if (realized) return
                    realized = true
                    if (shouldRealize) {
                        scope.realizeGroup(makeEnd)
                        realizeGroup()
                    } else {
                        scope.realizeCoalescableGroup()
                    }
                }
            }
        }

        class ClassScope(name: Name) : Scope("class ${name.asString()}")
        class PropertyScope(name: Name) : Scope("val ${name.asString()}")
        class FieldScope(name: Name) : Scope("field ${name.asString()}")
        class FileScope(val declaration: IrFile) : Scope("file ${declaration.name}") {
            override val fileScope: FileScope get() = this
        }

        class LoopScope(val loop: IrLoop) : BlockScope("loop") {
            private val jumpEndLocations = mutableListOf<(IrExpression) -> Unit>()
            var needsGroupPerIteration = false
                private set

            override fun sourceLocationOf(call: IrElement): SourceLocation {
                return object : SourceLocation(call) {
                    override val repeatable: Boolean
                        // the calls in the group only repeat if the loop scope doesn't create
                        // a group per iteration
                        get() = !needsGroupPerIteration
                }
            }

            fun markJump(jump: IrBreakContinue, extraEndLocation: (IrExpression) -> Unit) {
                if (jump.loop != loop) {
                    super.markJump(extraEndLocation)
                } else {
                    hasJump = true
                    // if there is a continue jump in the loop, it means that the repeating
                    // pattern of the call graph can differ per iteration, which means that we will
                    // need to create a group for each iteration or else we could end up with slot
                    // table misalignment.
                    if (jump is IrContinue) needsGroupPerIteration = true
                    jumpEndLocations.push(extraEndLocation)
                }
            }

            override fun realizeEndCalls(makeEnd: () -> IrExpression) {
                super.realizeEndCalls(makeEnd)
                if (needsGroupPerIteration) {
                    jumpEndLocations.fastForEach {
                        it(makeEnd())
                    }
                    jumpEndLocations.clear()
                }
            }
        }

        class KeyScope : BlockScope("key")
        class WhenScope : BlockScope("when")
        class BranchScope : BlockScope("branch")
        class CaptureScope : BlockScope("capture") {
            var hasCapturedComposableCall = false
                private set

            fun markCapturedComposableCall() {
                hasCapturedComposableCall = true
            }

            override fun sourceLocationOf(call: IrElement): SourceLocation =
                object : SourceLocation(call) {
                    override val repeatable: Boolean
                        get() = true
                }
        }

        class ParametersScope : BlockScope("parameters")

        class CallScope(
            val expression: IrFunctionAccessExpression,
            private val transformer: ComposableFunctionBodyTransformer,
        ) : Scope("call") {
            override val isInComposable: Boolean
                get() = parent?.isInComposable == true

            var marker: IrVariable? = null
                private set

            fun allocateMarker(): IrVariable = marker
                ?: transformer.irTemporary(
                    transformer.irCurrentMarker(myComposer),
                    getNameForTemporary("marker")
                ).also { marker = it }

            private fun getNameForTemporary(nameHint: String?) =
                functionScope?.getNameForTemporary(nameHint)
                    ?: error("Expected to be in a function")
        }

        class ReturnScope(
            val expression: IrReturn,
        ) : BlockScope("return") {
            override fun sourceLocationOf(call: IrElement): SourceLocation =
                when (val parent = parent) {
                    is BlockScope -> parent.sourceLocationOf(call)
                    else -> super.sourceLocationOf(call)
                }
        }
    }

    inner class IrDefaultBitMaskValueImpl(
        private val params: List<IrValueParameter>,
        private val count: Int,
    ) : IrDefaultBitMaskValue {

        init {
            val actual = params.size
            val expected = defaultParamCount(count)
            require(actual == expected) {
                "Function with $count params had $actual default params but expected $expected"
            }
        }

        override fun irIsolateBitAtIndex(index: Int): IrExpression {
            require(index <= count)
            // (%default and 0b1)
            return irAnd(
                // a value of 1 in default means it was NOT provided
                irGet(params[defaultsParamIndex(index)]),
                irConst(0b1 shl defaultsBitIndex(index))
            )
        }

        override fun irHasAnyProvidedAndUnstable(unstable: BooleanArray): IrExpression {
            require(count == unstable.size)
            val expressions = params.mapIndexed { index, param ->
                val start = index * BITS_PER_INT
                val end = min(start + BITS_PER_INT, count)
                val unstableMask = bitMask(*unstable.sliceArray(start until end))
                irNotEqual(
                    // $default and unstableMask will be different from unstableMask
                    // iff any parameters were *provided* AND *unstable*
                    irAnd(
                        irGet(param),
                        irConst(unstableMask)
                    ),
                    irConst(unstableMask)
                )
            }
            return if (expressions.size == 1)
                expressions.single()
            else
                expressions.reduce { lhs, rhs -> irOrOr(lhs, rhs) }
        }

        override fun putAsValueArgumentIn(fn: IrFunctionAccessExpression, startIndex: Int) {
            params.fastForEachIndexed { i, param ->
                fn.arguments[startIndex + i] = irGet(param)
            }
        }
    }

    open inner class IrChangedBitMaskValueImpl(
        private val params: List<IrValueDeclaration>,
        private val count: Int,
    ) : IrChangedBitMaskValue {
        protected fun paramIndexForSlot(slot: Int): Int = slot / SLOTS_PER_INT

        init {
            val actual = params.size
            // passing in 0 for thisParams because slot count includes them
            val expected = changedParamCount(count, 0)
            require(actual == expected) {
                "Function with $count params had $actual changed params but expected $expected"
            }
        }

        override var used: Boolean = false

        override val declarations: List<IrValueDeclaration>
            get() = params

        override fun irLowBit(): IrExpression {
            used = true
            return irAnd(
                irGet(params[0]),
                irConst(0b1)
            )
        }

        override fun irIsolateBitsAtSlot(slot: Int, includeStableBit: Boolean): IrExpression {
            used = true
            // %changed and 0b11
            return irAnd(
                irGet(params[paramIndexForSlot(slot)]),
                irBitsForSlot(
                    if (includeStableBit)
                        ParamState.Mask.bits
                    else
                        ParamState.Static.bits,
                    slot
                )
            )
        }

        override fun irStableBitAtSlot(slot: Int): IrExpression {
            used = true
            // %changed and 0b100
            return irAnd(
                irGet(params[paramIndexForSlot(slot)]),
                irBitsForSlot(0b100, slot)
            )
        }

        override fun irSlotAnd(slot: Int, bits: Int): IrExpression {
            used = true
            // %changed and 0b11
            return irAnd(
                irGet(params[paramIndexForSlot(slot)]),
                irBitsForSlot(bits, slot)
            )
        }

        // The restart flag is always in the first parameter flags (or the implied changed parameter for 0 parameters)
        override fun irRestartFlags(): IrExpression = irAnd(irGet(params[0]), irConst(1))

        override fun irHasDifferences(
            usedParams: BooleanArray,
        ): IrExpression {
            used = true
            require(usedParams.size == count)
            if (count == 0) {
                // for 0 slots (no params), we can create a shortcut expression of just checking the
                // low-bit for non-zero. Since all of the higher bits will also be 0, we can just
                // simplify this to check if dirty is non-zero
                return irNotEqual(
                    irGet(params[0]),
                    irConst(0)
                )
            }

            val expressions = params.mapIndexed { index, param ->
                val start = index * SLOTS_PER_INT
                val end = min(start + SLOTS_PER_INT, count)

                // makes an int with each slot having 0b101 mask and the low bit being 0.
                // so for 3 slots, we would get 0b 101 101 101 0.
                // This pattern is useful because we can and + xor it with our $changed bitmask and it
                // will only be non-zero if any of the slots were DIFFERENT or UNCERTAIN or
                // UNSTABLE.
                // we _only_ use this pattern for the slots where the body of the function
                // actually uses that parameter, otherwise we pass in 0b000 which will transfer
                // none of the bits to the rhs
                val lhsMask = if (FeatureFlag.StrongSkipping.enabled) 0b001 else 0b101
                val lhs = (start until end).fold(0) { mask, slot ->
                    if (usedParams[slot]) mask or bitsForSlot(lhsMask, slot) else mask
                }

                // we _only_ use this pattern for the slots where the body of the function
                // actually uses that parameter, otherwise we pass in 0b000 which will transfer
                // none of the bits to the rhs
                val rhs = (start until end).fold(0) { mask, slot ->
                    if (usedParams[slot]) mask or bitsForSlot(0b001, slot) else mask
                }

                // we use this pattern with the low bit set to 1 in the "and", and the low bit set to 0
                // for the "xor". This means that if the low bit was set, we will get 1 in the resulting
                // low bit. Since we use this calculation to determine if we need to run the body of the
                // function, this is exactly what we want.

                // if the rhs is 0, that means that none of the parameters ended up getting used
                // in the body of the function which means we can simplify the expression quite a
                // bit. In this case we just care about if the low bit is non-zero
                if (rhs == 0) {
                    irNotEqual(
                        irAnd(
                            irGet(param),
                            irConst(1)
                        ),
                        irConst(0)
                    )
                } else {
                    // $dirty and (0b 101 ... 101 1) != (0b 001 ... 001 0)
                    irNotEqual(
                        irAnd(
                            irGet(param),
                            irConst(lhs or 0b1)
                        ),
                        irConst(rhs or 0b0)
                    )
                }
            }
            return if (expressions.size == 1)
                expressions.single()
            else
                expressions.reduce { lhs, rhs -> irOrOr(lhs, rhs) }
        }

        override fun irCopyToTemporary(
            nameHint: String?,
            isVar: Boolean,
            exactName: Boolean,
        ): IrChangedBitMaskVariable {
            used = true
            val temps = params.mapIndexed { index, param ->
                IrVariableImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    // We label "dirty" as a defined variable instead of a temporary, so that it
                    // is properly stored in the locals table and discoverable by debuggers. The
                    // dirty variable encodes information that could be useful for tooling to
                    // interpret.
                    IrDeclarationOrigin.DEFINED,
                    IrVariableSymbolImpl(),
                    Name.identifier(if (index == 0) "\$dirty" else "\$dirty$index"),
                    param.type,
                    isVar,
                    isConst = false,
                    isLateinit = false
                ).apply {
                    parent = currentFunctionScope.function.parent
                    initializer = irGet(param)
                }
            }
            return IrChangedBitMaskVariableImpl(temps, count)
        }

        override fun putAsValueArgumentInWithLowBit(
            fn: IrFunctionAccessExpression,
            startIndex: Int,
            lowBit: Boolean,
        ) {
            used = true
            params.fastForEachIndexed { index, param ->
                fn.arguments[startIndex + index] =
                    if (index == 0) {
                        irUpdateChangedFlags(irOr(irGet(param), irConst(if (lowBit) 0b1 else 0b0)))
                    } else {
                        irUpdateChangedFlags(irGet(param))
                    }
            }
        }

        private fun irUpdateChangedFlags(expression: IrExpression): IrExpression {
            return updateChangedFlagsFunction?.let {
                irCall(it).also {
                    it.arguments[0] = expression
                }
            } ?: expression
        }

        override fun irShiftBits(fromSlot: Int, toSlot: Int): IrExpression {
            used = true
            val fromSlotAdjusted = fromSlot.rem(SLOTS_PER_INT)
            val toSlotAdjusted = toSlot.rem(SLOTS_PER_INT)
            val bitsToShiftLeft = (toSlotAdjusted - fromSlotAdjusted) * BITS_PER_SLOT
            val value = irGet(params[paramIndexForSlot(fromSlot)])

            if (bitsToShiftLeft == 0) return value
            val int = context.irBuiltIns.intType
            val shiftLeft = int.binaryOperator(
                OperatorNameConventions.SHL,
                int
            )
            val shiftRight = int.binaryOperator(
                OperatorNameConventions.SHR,
                int
            )

            return irCall(
                symbol = if (bitsToShiftLeft > 0) shiftLeft else shiftRight,
                dispatchReceiver = value,
                args = arrayOf(irConst(abs(bitsToShiftLeft)))
            )
        }
    }

    inner class IrChangedBitMaskVariableImpl(
        private val temps: List<IrVariable>,
        count: Int,
    ) : IrChangedBitMaskVariable, IrChangedBitMaskValueImpl(temps, count) {
        override fun asStatements(): List<IrStatement> {
            return temps
        }

        override fun irOrSetBitsAtSlot(slot: Int, value: IrExpression): IrExpression {
            used = true
            val temp = temps[paramIndexForSlot(slot)]
            return irSet(
                temp,
                irOr(
                    irGet(temp),
                    value
                )
            )
        }

        override fun irSetSlotUncertain(slot: Int): IrExpression {
            used = true
            val temp = temps[paramIndexForSlot(slot)]
            return irSet(
                temp,
                irAnd(
                    irGet(temp),
                    irConst(ParamState.Mask.bitsForSlot(slot).inv())
                )
            )
        }
    }
}

private fun String.replacePrefix(prefix: String, replacement: String) =
    if (startsWith(prefix)) replacement + substring(prefix.length) else this

private fun IrFunction.isLambda(): Boolean {
    // There is probably a better way to determine this, but if there is, it isn't obvious
    return name == SpecialNames.ANONYMOUS
}

inline fun <A, B, C> forEachWith(a: List<A>, b: List<B>, c: List<C>, fn: (A, B, C) -> Unit) {
    for (i in a.indices) {
        fn(a[i], b[i], c[i])
    }
}

inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    for (i in indices) {
        val item = get(i)
        action(item)
    }
}

inline fun <T> List<T>.fastForEachIndexed(action: (index: Int, T) -> Unit) {
    for (i in indices) {
        val item = get(i)
        action(i, item)
    }
}

inline fun <T> Array<out T>.fastForEachIndexed(action: (index: Int, T) -> Unit) {
    for (i in indices) {
        val item = get(i)
        action(i, item)
    }
}

private fun IrType.isClassType(fqName: FqNameUnsafe, hasQuestionMark: Boolean? = null): Boolean {
    if (this !is IrSimpleType) return false
    if (hasQuestionMark != null && this.isMarkedNullable() == hasQuestionMark) return false
    return classifier.isClassWithFqName(fqName)
}

private fun IrType.isNotNullClassType(fqName: FqNameUnsafe) =
    isClassType(fqName, hasQuestionMark = false)

private fun IrType.isNullableClassType(fqName: FqNameUnsafe) =
    isClassType(fqName, hasQuestionMark = true)

fun IrType.isNullableUnit() = isNullableClassType(StandardNames.FqNames.unit)
fun IrType.isUnitOrNullableUnit() = this.isUnit() || this.isNullableUnit()

internal object UNINITIALIZED_VALUE

private fun mutableStatementContainer(context: IrPluginContext): IrContainerExpression {
    // NOTE(lmr): It's important to use IrComposite here so that we don't introduce any new
    // scopes
    return IrCompositeImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        context.irBuiltIns.unitType
    )
}

private fun IrFunction.callInformation(): String =
    buildString {
        append('C')
        if (isInline) append('C')
        if (!isLambda()) {
            append('(')
            append(name.asString())
            append(')')
        }
    }

// Parameter information is an index from the sorted order of the parameters to the
// actual order. This is used to reorder the fields of the lambda class generated for
// restart lambdas into parameter order. If all the parameters are in sorted order
// with no inline classes then no additional information is necessary. This means
// that parameter-less or single parameter functions with no inline classes never
// need additional information and two parameter functions are only 50% likely to
// need ordering information which is, if needed, very short ("1"). The encoding is as
// follows,
//
//   parameters: (parameter|run) ("," parameter | run)*
//   parameter: sorted-index [":" inline-class]
//   sorted-index: <number>
//   inline-class: <chars not "," or "!">
//   run: "!" <number>
//
//   where
//     sorted-index:  the index of the parameter's name in the sorted list of
//                    parameter names,
//     inline-class:  the fully qualified name of the inline class using "c#" as a
//                    short-hand for "androidx.compose.".
//     run:           The number of parameter that are in sequence assuming the
//                    previously selected parameters are removed from the sorted order.
//                    For example, "!5" at the beginning of the list is equivalent to
//                    "0,1,2,3,4" and "3!4" is equivalent to "3,0,1,2,4". If there
//                    are 9 parameters "3,4!2,6,8" is equivalent to "3,4,0,1,6,8,2,
//                    5,6,7".
//
// There is an implied "!n" (where n is the number of remaining parameters) at the end
// of the parameter information that implies the rest of the parameters are in order.
// If the parameter information is missing it implies "P()" which implies all the
// parameters are in sorted order.
private fun IrFunction.parameterInformation(): String {
    val builder = StringBuilder("P(")
    val parameters = namedParameters.filter {
        !it.name.asString().startsWith("$")
    }
    val sortIndex = mapOf(
        *parameters.mapIndexed { index, parameter ->
            Pair(index, parameter)
        }.sortedBy { it.second.name.asString() }
            .mapIndexed { sortIndex, originalIndex ->
                Pair(originalIndex.first, sortIndex)
            }.toTypedArray()
    )

    val expectedIndexes = Array(parameters.size) { it }.toMutableList()
    var run = 0
    var parameterEmitted = false

    fun emitRun(originalIndex: Int) {
        if (run > 0) {
            builder.append('!')
            if (originalIndex < parameters.size - 1) {
                builder.append(run)
            }
            run = 0
        }
    }

    parameters.fastForEachIndexed { originalIndex, parameter ->
        if (expectedIndexes.first() == sortIndex[originalIndex] &&
            !parameter.type.isInlineClassType()
        ) {
            run++
            expectedIndexes.removeAt(0)
        } else {
            emitRun(originalIndex)
            if (originalIndex > 0) builder.append(',')
            val index = sortIndex[originalIndex]
                ?: error("missing index $originalIndex")
            builder.append(index)
            expectedIndexes.remove(index)
            if (parameter.type.isInlineClassType()) {
                parameter.type.getClass()?.fqNameWhenAvailable?.let {
                    builder.append(':')
                    builder.append(
                        it.asString()
                            .replacePrefix("androidx.compose.", "c#")
                    )
                }
            }
            parameterEmitted = true
        }
    }
    builder.append(')')
    return if (parameterEmitted) builder.toString() else ""
}

private fun IrFunction.packageName(): String? {
    var parent = parent
    while (true) {
        when (parent) {
            is IrPackageFragment -> return parent.packageFqName.asString()
            is IrDeclaration -> parent = parent.parent
            else -> break
        }
    }
    return null
}

private fun IrFunction.packageHash(): Int =
    packageName()?.fold(0) { hash, current ->
        hash * 31 + current.code
    }?.absoluteValue ?: 0

private fun IrFunction.sourceFileInformation(): String {
    val hash = packageHash()
    if (hash != 0)
        return "${file.name}#${hash.toString(36)}"
    return file.name
}
