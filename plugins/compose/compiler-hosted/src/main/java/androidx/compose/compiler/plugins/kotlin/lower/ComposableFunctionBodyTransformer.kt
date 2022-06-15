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
import androidx.compose.compiler.plugins.kotlin.FunctionMetrics
import androidx.compose.compiler.plugins.kotlin.KtxNameConventions
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices
import androidx.compose.compiler.plugins.kotlin.analysis.Stability
import androidx.compose.compiler.plugins.kotlin.analysis.knownStable
import androidx.compose.compiler.plugins.kotlin.analysis.knownUnstable
import androidx.compose.compiler.plugins.kotlin.hasExplicitGroupsAnnotation
import androidx.compose.compiler.plugins.kotlin.hasNonRestartableComposableAnnotation
import androidx.compose.compiler.plugins.kotlin.hasReadonlyComposableAnnotation
import androidx.compose.compiler.plugins.kotlin.irTrace
import androidx.compose.compiler.plugins.kotlin.lower.decoys.DecoyFqNames
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KProperty
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrLocalDelegatedProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrContinue
import org.jetbrains.kotlin.ir.expressions.IrDoWhileLoop
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSpreadElementImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhenImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrReturnTargetSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isByte
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isClassWithFqName
import org.jetbrains.kotlin.ir.types.isDouble
import org.jetbrains.kotlin.ir.types.isFloat
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.types.isNullableNothing
import org.jetbrains.kotlin.ir.types.isShort
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

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
 *
 * Note that extension and dispatch receiver params will not show up in [IrFunction.valueParameters]
 * but context receiver parameter ([IrFunction.contextReceiverParametersCount]) will.
 */
val IrFunction.thisParamCount
    get() = contextReceiverParametersCount +
        (if (dispatchReceiverParameter != null) 1 else 0) +
        (if (extensionReceiverParameter != null) 1 else 0)

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
 * @param valueParams The numbers of params, usually the size of [IrFunction.valueParameters].
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

interface IrChangedBitMaskValue {
    val used: Boolean
    fun irLowBit(): IrExpression
    fun irIsolateBitsAtSlot(slot: Int, includeStableBit: Boolean): IrExpression
    fun irSlotAnd(slot: Int, bits: Int): IrExpression
    fun irHasDifferences(usedParams: BooleanArray): IrExpression
    fun irCopyToTemporary(
        nameHint: String? = null,
        isVar: Boolean = false,
        exactName: Boolean = false
    ): IrChangedBitMaskVariable
    fun putAsValueArgumentInWithLowBit(
        fn: IrFunctionAccessExpression,
        startIndex: Int,
        lowBit: Boolean
    )
    fun irShiftBits(fromSlot: Int, toSlot: Int): IrExpression
}

interface IrDefaultBitMaskValue {
    fun irIsolateBitAtIndex(index: Int): IrExpression
    fun irHasAnyProvidedAndUnstable(unstable: BooleanArray): IrExpression
    fun putAsValueArgumentIn(fn: IrFunctionAccessExpression, startIndex: Int)
}

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
 * 1. Replaceable Groups
 * 2. Movable Groups
 * 3. Restart Groups
 *
 * Generally speaking, every composable function *must* emit a single group when it executes.
 * Every group can have any number of children groups. Additionally, we analyze each executable
 * block and apply the following rules:
 *
 * 1. If a block executes exactly 1 time always, no groups are needed
 * 2. If a set of blocks are such that exactly one of them is executed exactly once (for example,
 * the result blocks of a when clause), then we insert a replaceable group around each block.
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
 *       if ($changed and 0b0110 === 0) {
 *         $dirty = $dirty or if ($composer.changed(x)) 0b0010 else 0b0100
 *       }
 *      if (%dirty and 0b1011 !== 0b1010 || !$composer.skipping) {
 *        f(x)
 *      } else {
 *        $composer.skipToGroupEnd()
 *      }
 *     }
 *
 * Note that this makes use of bitmasks for the $changed and $dirty values. These bitmasks work
 * in a different bit-space than the $default bitmask because two bits are needed to hold the
 * four different possible states of each parameter. Additionally, the lowest bit of the bitmask
 * is a special bit which forces execution of the function.
 *
 * This means that for the ith parameter of a composable function, the bit range of i*2 + 1 to
 * i*2 + 2 are used to store the state of the parameter.
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
@Suppress("DEPRECATION")
class ComposableFunctionBodyTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    metrics: ModuleMetrics,
    sourceInformationEnabled: Boolean,
    private val intrinsicRememberEnabled: Boolean
) :
    AbstractComposeLowering(context, symbolRemapper, bindingTrace, metrics),
    FileLoweringPass,
    ModuleLoweringPass {

    private var inlineLambdaInfo = ComposeInlineLambdaLocator(context)

    override fun lower(module: IrModuleFragment) {
        inlineLambdaInfo.scan(module)
        module.transformChildrenVoid(this)
        applySourceFixups()
        module.patchDeclarationParents()
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
        applySourceFixups()
    }

    private val changedFunction = composerIrClass.functions
        .first {
            it.name.identifier == "changed" && it.valueParameters.first().type.isNullableAny()
        }

    private fun IrType.toPrimitiveType(): PrimitiveType? = when {
        isInt() -> PrimitiveType.INT
        isBoolean() -> PrimitiveType.BOOLEAN
        isFloat() -> PrimitiveType.FLOAT
        isLong() -> PrimitiveType.LONG
        isDouble() -> PrimitiveType.DOUBLE
        isByte() -> PrimitiveType.BYTE
        isChar() -> PrimitiveType.CHAR
        isShort() -> PrimitiveType.SHORT
        else -> null
    }

    private val changedPrimitiveFunctions by guardedLazy {
        composerIrClass
            .functions
            .filter { it.name.identifier == "changed" }
            .mapNotNull { f ->
                f.valueParameters.first().type.toPrimitiveType()?.let { primitive ->
                    primitive to f
                }
            }
            .toMap()
    }

    private val skipToGroupEndFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "skipToGroupEnd" && it.valueParameters.size == 0
            }
    }

    private val skipCurrentGroupFunction by guardedLazy {
        composerIrClass
            .functions
            .first {
                it.name.identifier == "skipCurrentGroup" && it.valueParameters.size == 0
            }
    }

    private val startReplaceableFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "startReplaceableGroup" && it.valueParameters.size == 1
            }
    }

    private val endReplaceableFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "endReplaceableGroup" && it.valueParameters.size == 0
            }
    }

    private val startDefaultsFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "startDefaults" && it.valueParameters.size == 0
            }
    }

    private val endDefaultsFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "endDefaults" && it.valueParameters.size == 0
            }
    }

    private val startMovableFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "startMovableGroup" && it.valueParameters.size == 2
            }
    }

    private val endMovableFunction by guardedLazy {
        composerIrClass.functions
            .first {
                it.name.identifier == "endMovableGroup" && it.valueParameters.size == 0
            }
    }

    private val startRestartGroupFunction by guardedLazy {
        composerIrClass
            .functions
            .first {
                it.name == KtxNameConventions.STARTRESTARTGROUP && it.valueParameters.size == 1
            }
    }

    private val endRestartGroupFunction by guardedLazy {
        composerIrClass
            .functions
            .first {
                it.name == KtxNameConventions.ENDRESTARTGROUP && it.valueParameters.size == 0
            }
    }

    private val sourceInformationFunction by guardedLazy {
        getTopLevelFunctions(
            ComposeFqNames.fqNameFor(KtxNameConventions.SOURCEINFORMATION)
        ).map { it.owner }.first()
    }

    private val sourceInformationMarkerStartFunction by guardedLazy {
        getTopLevelFunctions(
            ComposeFqNames.fqNameFor(KtxNameConventions.SOURCEINFORMATIONMARKERSTART)
        ).map { it.owner }.first()
    }

    private val isTraceInProgressFunction by guardedLazy {
        getTopLevelFunctions(
            ComposeFqNames.fqNameFor(KtxNameConventions.IS_TRACE_IN_PROGRESS)
        ).map { it.owner }.singleOrNull {
            it.valueParameters.isEmpty()
        }
    }

    private val traceEventStartFunction by guardedLazy {
        getTopLevelFunctions(
            ComposeFqNames.fqNameFor(KtxNameConventions.TRACE_EVENT_START)
        ).map { it.owner }.singleOrNull {
            it.valueParameters.map { p -> p.type } == listOf(
                context.irBuiltIns.intType,
                context.irBuiltIns.intType,
                context.irBuiltIns.intType,
                context.irBuiltIns.stringType
            )
        }
    }

    private val traceEventEndFunction by guardedLazy {
        getTopLevelFunctions(
            ComposeFqNames.fqNameFor(KtxNameConventions.TRACE_EVENT_END)
        ).map { it.owner }.singleOrNull {
            it.valueParameters.isEmpty()
        }
    }

    private val sourceInformationMarkerEndFunction by guardedLazy {
        getTopLevelFunctions(
            ComposeFqNames.fqNameFor(KtxNameConventions.SOURCEINFORMATIONMARKEREND)
        ).map { it.owner }.first()
    }

    private val IrType.arguments: List<IrTypeArgument>
        get() = (this as? IrSimpleType)?.arguments.orEmpty()

    private val updateScopeFunction by guardedLazy {
        endRestartGroupFunction.returnType
            .classOrNull
            ?.owner
            ?.functions
            ?.singleOrNull {
                it.name == KtxNameConventions.UPDATE_SCOPE &&
                    it.valueParameters.first().type.arguments.size == 3
            }
            ?: error("new updateScope not found in result type of endRestartGroup")
    }

    private val updateScopeBlockType by guardedLazy {
        updateScopeFunction
            .valueParameters
            .single()
            .type
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
                it.name == KtxNameConventions.JOINKEY && it.valueParameters.size == 2
            }
    }

    private val cacheFunction by guardedLazy {
        getTopLevelFunctions(ComposeFqNames.fqNameFor("cache")).map { it.owner }.first {
            it.valueParameters.size == 2 && it.extensionReceiverParameter != null
        }
    }

    private var currentScope: Scope = Scope.RootScope()

    private fun printScopeStack(): String {
        return buildString {
            currentScope.forEach {
                appendln(it.name)
            }
        }
    }

    private val isInComposableScope: Boolean
        get() = currentScope.isInComposable

    private val currentFunctionScope
        get() = currentScope.functionScope
            ?: error("Expected a FunctionScope but none exist. \n${printScopeStack()}")

    private val collectSourceInformation = sourceInformationEnabled

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
            (declaration as? IrAttributeContainer)?.let {
                context.irTrace.record(ComposeWritableSlices.FUNCTION_METRICS, it, scope.metrics)
            }
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun visitFunctionInScope(declaration: IrFunction): IrStatement {
        val scope = currentFunctionScope
        // if the function isn't composable, there's nothing to do
        if (!scope.isComposable) return super.visitFunction(declaration)
        val restartable = declaration.shouldBeRestartable()
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
        }
    }

    // Currently, we make all composable functions restartable by default, unless:
    // 1. They are inline
    // 2. They have a return value (may get relaxed in the future)
    // 3. They are a lambda (we use ComposableLambda<...> class for this instead)
    // 4. They are annotated as @NonRestartableComposable
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.shouldBeRestartable(): Boolean {
        // Only insert observe scopes in non-empty composable function
        if (body == null)
            return false

        if (isLocal && parentClassOrNull?.origin != JvmLoweredDeclarationOrigin.LAMBDA_IMPL) {
            return false
        }

        val descriptor = descriptor

        // Do not insert observe scope in an inline function
        if (descriptor.isInline)
            return false

        if (descriptor.hasNonRestartableComposableAnnotation())
            return false

        if (descriptor.hasExplicitGroupsAnnotation())
            return false

        // Do not insert an observe scope in an inline composable lambda
        if (inlineLambdaInfo.isInlineLambda(this)) return false

        // Do not insert an observe scope if the function has a return result
        if (descriptor.returnType.let { it == null || !it.isUnit() })
            return false

        // Do not insert an observe scope if the function hasn't been transformed by the
        // ComposerParamTransformer and has a synthetic "composer param" as its last parameter
        if (composerParam() == null) return false

        // Check if the descriptor has restart scope calls resolved
        if (descriptor is SimpleFunctionDescriptor &&
            // Lambdas should be ignored. All composable lambdas are wrapped by a restartable
            // function wrapper by ComposerLambdaMemoization which supplies the startRestartGroup/
            // endRestartGroup pair on behalf of the lambda.
            origin != IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        ) {

            return true
        }

        return false
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.shouldElideGroups(): Boolean {
        return descriptor.hasReadonlyComposableAnnotation() ||
            descriptor.hasExplicitGroupsAnnotation()
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrFunction.isReadonly(): Boolean {
        return descriptor.hasReadonlyComposableAnnotation()
    }

    // At a high level, a non-restartable composable function
    // 1. gets a replaceable group placed around the body
    // 2. never calls `$composer.changed(...)` with its parameters
    // 3. can have default parameters, so needs to add the defaults preamble if defaults present
    // 4. proper groups around control flow structures in the body
    @ObsoleteDescriptorBasedAPI
    private fun visitNonRestartableComposableFunction(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?
    ): IrStatement {
        val body = declaration.body!!

        val elideGroups = declaration.shouldElideGroups()

        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        scope.dirty = changedParam

        val defaultScope = transformDefaults(scope)

        var (transformed, returnVar) = body.asBodyAndResultVar()

        transformed = transformed.apply { transformChildrenVoid() }

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

        if (!elideGroups) scope.realizeGroup(::irEndReplaceableGroup)

        declaration.body = IrBlockBodyImpl(
            body.startOffset,
            body.endOffset,
            listOfNotNull(
                when {
                    !elideGroups ->
                        irStartReplaceableGroup(
                            body,
                            scope,
                            irFunctionSourceKey()
                        )
                    collectSourceInformation &&
                        !declaration.descriptor.hasExplicitGroupsAnnotation() ->
                        irSourceInformationMarkerStart(
                            body,
                            scope,
                            irFunctionSourceKey()
                        )
                    else -> null
                },
                *bodyPreamble.statements.toTypedArray(),
                *transformed.statements.toTypedArray(),
                when {
                    !elideGroups -> irEndReplaceableGroup()
                    collectSourceInformation &&
                        !declaration.descriptor.hasExplicitGroupsAnnotation() ->
                        irSourceInformationMarkerEnd(body)
                    else -> null
                },
                returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
            )
        )
        if (
            elideGroups &&
            collectSourceInformation &&
            !declaration.descriptor.hasExplicitGroupsAnnotation()
        ) {
            scope.realizeEndCalls {
                irSourceInformationMarkerEnd(body)
            }
        }

        scope.metrics.recordFunction(
            composable = true,
            restartable = false,
            skippable = false,
            isLambda = declaration.isLambda(),
            inline = declaration.isInline,
            hasDefaults = false,
            readonly = elideGroups,
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
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun visitComposableLambda(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue
    ): IrStatement {
        // no group, since composableLambda should already create one
        // no default logic
        val body = declaration.body!!
        val sourceInformationPreamble = mutableStatementContainer()
        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        // First generate the source information call
        if (collectSourceInformation && !scope.isInlinedLambda) {
            sourceInformationPreamble.statements.add(irSourceInformation(scope))
        }

        // we start off assuming that we *can* skip execution of the function
        var canSkipExecution = declaration.returnType.isUnit() &&
            scope.allTrackedParams.none { stabilityOf(it.type).knownUnstable() }

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
                isVar = if (context.platform.isJvm() || context.platform.isJs()) false else true,
                nameHint = "\$dirty",
                exactName = true
            )
        else
            changedParam

        scope.dirty = dirty

        val (nonReturningBody, returnVar) = body.asBodyAndResultVar()

        // we must transform the body first, since that will allow us to see whether or not we
        // are using the dispatchReceiverParameter or the extensionReceiverParameter
        val transformed = nonReturningBody.apply { transformChildrenVoid() }

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

        val dirtyForSkipping = if (dirty.used && dirty is IrChangedBitMaskVariable) {
            skipPreamble.statements.addAll(0, dirty.asStatements())
            dirty
        } else changedParam
        if (canSkipExecution) {
            // We CANNOT skip if any of the following conditions are met
            // 1. if any of the stable parameters have *differences* from last execution.
            // 2. if the composer.skipping call returns false
            val shouldExecute = irOrOr(
                dirtyForSkipping.irHasDifferences(scope.usedParams),
                irNot(irIsSkipping())
            )

            val transformedBody = irIfThenElse(
                condition = shouldExecute,
                thenPart = irBlock(
                    type = context.irBuiltIns.unitType,
                    statements = transformed.statements
                ),
                // Use end offsets so that stepping out of the composable function
                // does not step back to the start line for the function.
                elsePart = irSkipToGroupEnd(body.endOffset, body.endOffset),
                startOffset = body.startOffset,
                endOffset = body.endOffset
            )
            scope.realizeCoalescableGroup()
            declaration.body = IrBlockBodyImpl(
                body.startOffset,
                body.endOffset,
                listOfNotNull(
                    if (collectSourceInformation && scope.isInlinedLambda)
                        irStartReplaceableGroup(body, scope, irFunctionSourceKey())
                    else null,
                    *sourceInformationPreamble.statements.toTypedArray(),
                    *skipPreamble.statements.toTypedArray(),
                    *bodyPreamble.statements.toTypedArray(),
                    transformedBody,
                    if (collectSourceInformation && scope.isInlinedLambda)
                        irEndReplaceableGroup()
                    else null,
                    returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
                )
            )
        } else {
            declaration.body = IrBlockBodyImpl(
                body.startOffset,
                body.endOffset,
                listOfNotNull(
                    *sourceInformationPreamble.statements.toTypedArray(),
                    *skipPreamble.statements.toTypedArray(),
                    *bodyPreamble.statements.toTypedArray(),
                    transformed,
                    returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
                )
            )
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
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun visitRestartableComposableFunction(
        declaration: IrFunction,
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?
    ): IrStatement {
        val body = declaration.body!!
        val skipPreamble = mutableStatementContainer()
        val bodyPreamble = mutableStatementContainer()

        // we start off assuming that we *can* skip execution of the function
        var canSkipExecution = true

        // NOTE(lmr): Technically, dirty is a mutable variable, but we don't want to mark it
        // as one since that will cause a `Ref<Int>` to get created if it is captured. Since
        // we know we will never be mutating this variable _after_ it gets captured, we can
        // safely mark this as `isVar = false`.
        val dirty = if (scope.allTrackedParams.isNotEmpty())
            changedParam.irCopyToTemporary(
                // LLVM validation doesn't allow us to have val here.
                isVar = if (context.platform.isJvm() || context.platform.isJs()) false else true,
                nameHint = "\$dirty",
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

        val defaultScope = transformDefaults(scope)

        // we must transform the body first, since that will allow us to see whether or not we
        // are using the dispatchReceiverParameter or the extensionReceiverParameter
        val transformed = nonReturningBody.apply { transformChildrenVoid() }

        canSkipExecution = buildPreambleStatementsAndReturnIfSkippingPossible(
            body,
            skipPreamble,
            bodyPreamble,
            canSkipExecution,
            scope,
            dirty,
            changedParam,
            defaultParam,
            defaultScope,
        )

        // if it has non-optional unstable params, the function can never skip, so we always
        // execute the body. Otherwise, we wrap the body in an if and only skip when certain
        // conditions are met.
        val dirtyForSkipping = if (dirty.used && dirty is IrChangedBitMaskVariable) {
            skipPreamble.statements.addAll(0, dirty.asStatements())
            dirty
        } else changedParam
        val transformedBody = if (canSkipExecution) {
            // We CANNOT skip if any of the following conditions are met
            // 1. if any of the stable parameters have *differences* from last execution.
            // 2. if the composer.skipping call returns false
            // 3. if any of the provided parameters to the function were unstable

            // (3) is only necessary to check if we actually have unstable params, so we only
            // generate that check if we need to.
            var shouldExecute = irOrOr(
                dirtyForSkipping.irHasDifferences(scope.usedParams),
                irNot(irIsSkipping())
            )

            // boolean array mapped to parameters. true indicates that the type is unstable
            // NOTE: the unstable mask is indexed by valueParameter index, which is different
            // than the slotIndex but that is OKAY because we only care about defaults, which
            // also use the value parameter index.
            val realParams = declaration.valueParameters.take(
                declaration.contextReceiverParametersCount + scope.realValueParamCount
            )
            val unstableMask = realParams.map {
                stabilityOf((it.varargElementType ?: it.type)).knownUnstable()
            }.toBooleanArray()

            val hasAnyUnstableParams = unstableMask.any { it }

            // if there are unstable params, then we fence the whole expression with a check to
            // see if any of the unstable params were the ones that were provided to the
            // function. If they were, then we short-circuit and always execute
            if (hasAnyUnstableParams && defaultParam != null) {
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
                elsePart = irSkipToGroupEnd(body.endOffset, body.endOffset),
                startOffset = body.startOffset,
                endOffset = body.endOffset
            )
        } else irComposite(
            statements = bodyPreamble.statements + transformed.statements
        )

        scope.realizeGroup(end)

        declaration.body = IrBlockBodyImpl(
            body.startOffset,
            body.endOffset,
            listOfNotNull(
                irTraceEventStart(irFunctionSourceKey(), declaration),
                irStartRestartGroup(
                    body,
                    scope,
                    irFunctionSourceKey()
                ),
                *skipPreamble.statements.toTypedArray(),
                transformedBody,
                if (returnVar == null) end() else null,
                returnVar?.let { irReturn(declaration.symbol, irGet(it)) }
            )
        )

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

    private class SourceInfoFixup(val call: IrCall, val index: Int, val scope: Scope.BlockScope)
    private val sourceFixups = mutableListOf<SourceInfoFixup>()

    private fun recordSourceParameter(call: IrCall, index: Int, scope: Scope.BlockScope) {
        sourceFixups.add(SourceInfoFixup(call, index, scope))
    }

    private val (Scope.BlockScope).hasSourceInformation get() =
        calculateHasSourceInformation(collectSourceInformation)

    private val (Scope.BlockScope).sourceInformation get() =
        calculateSourceInfo(collectSourceInformation)

    private fun applySourceFixups() {
        // Apply the fix-ups lowest scope to highest.
        sourceFixups.sortBy {
            -it.scope.level
        }
        for (sourceFixup in sourceFixups) {
            sourceFixup.call.putValueArgument(
                sourceFixup.index,
                irConst(sourceFixup.scope.sourceInformation ?: "")
            )
        }
        sourceFixups.clear()
    }

    private fun transformDefaults(scope: Scope.FunctionScope): Scope.ParametersScope {
        val parameters = scope.allTrackedParams
        val parametersScope = Scope.ParametersScope()
        parameters.forEach { param ->
            val defaultValue = param.defaultValue
            if (defaultValue != null) {
                defaultValue.expression = inScope(parametersScope) {
                    defaultValue.expression.transform(this, null)
                }
            }
        }
        return parametersScope
    }

    @ObsoleteDescriptorBasedAPI
    private fun buildPreambleStatementsAndReturnIfSkippingPossible(
        sourceElement: IrElement,
        skipPreamble: IrStatementContainer,
        bodyPreamble: IrStatementContainer,
        isSkippableDeclaration: Boolean,
        scope: Scope.FunctionScope,
        dirty: IrChangedBitMaskValue,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
        defaultScope: Scope.ParametersScope
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
//        val parametersScope = Scope.ParametersScope()
        parameters.forEachIndexed { slotIndex, param ->
            val defaultIndex = scope.defaultIndexForSlotIndex(slotIndex)
            val defaultValue = param.defaultValue?.expression
            if (defaultParam != null && defaultValue != null) {
//                val transformedDefault = inScope(parametersScope) {
//                    defaultValue.expression.transform(this, null)
//                }

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

        parameters.forEachIndexed { slotIndex, param ->
            val stability = stabilityOf(param.varargElementType ?: param.type)

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

            if (isUsed && isUnstable && isRequired) {
                // if it is a used + unstable parameter with no default expression, the fn
                // will _never_ skip
                mightSkip = false
            }
        }

        // we start the skipPreamble with all of the changed calls. These need to go at the top
        // of the function's group. Note that these end up getting called *before* default
        // expressions, but this is okay because it will only ever get called on parameters that
        // are provided to the function
        parameters.forEachIndexed { slotIndex, param ->
            // varargs get handled separately because they will require their own groups
            if (param.isVararg) return@forEachIndexed
            val defaultIndex = scope.defaultIndexForSlotIndex(slotIndex)
            val defaultValue = param.defaultValue
            val isUnstable = stabilities[slotIndex].knownUnstable()
            val isUsed = scope.usedParams[slotIndex]

            when {
                !mightSkip || !isUsed -> {
                    // nothing to do
                }
                dirty !is IrChangedBitMaskVariable -> {
                    // this will only ever be true when mightSkip is false, but we put this
                    // branch here so that `dirty` gets smart cast in later branches
                }
                isUnstable && defaultParam != null && defaultValue != null -> {
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
                !isUnstable -> {
                    val defaultValueIsStatic = defaultExprIsStatic[slotIndex]
                    val callChanged = irChanged(irGet(param))
                    val isChanged = if (defaultParam != null && !defaultValueIsStatic)
                        irAndAnd(irIsProvided(defaultParam, slotIndex), callChanged)
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

                    val stmt = if (defaultParam != null && defaultValueIsStatic) {
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
                                    condition = irIsUncertainAndStable(changedParam, slotIndex),
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
                            condition = irIsUncertainAndStable(changedParam, slotIndex),
                            body = modifyDirtyFromChangedResult
                        )
                    }
                    skipPreamble.statements.add(stmt)
                }
            }
        }

        // now we handle the vararg parameters specially since it needs to create a group
        parameters.forEachIndexed { slotIndex, param ->
            val varargElementType = param.varargElementType ?: return@forEachIndexed
            if (mightSkip && dirty is IrChangedBitMaskVariable) {
                // for vararg parameters of stable type, we can store each value in the slot
                // table, but need to generate a group since the size of the array could change
                // over time. In the future, we may want to make an optimization where whether or
                // not the call site had a spread or not and only create groups if it did.

                // composer.startMovableGroup(<>, values.size)
                val irGetParamSize = irMethodCall(
                    irGet(param),
                    param.type.classOrNull!!.getPropertyGetter("size")!!.owner
                )
                // TODO(lmr): verify this works with default vararg expressions!
                skipPreamble.statements.add(
                    irStartMovableGroup(
                        param,
                        irGetParamSize,
                        defaultScope,
                    )
                )

                // for (value in values) {
                //     dirty = dirty or if (composer.changed(value)) 0b0100 else 0b0000
                // }
                skipPreamble.statements.add(
                    irForLoop(
                        varargElementType,
                        irGet(param)
                    ) { loopVar ->
                        dirty.irOrSetBitsAtSlot(
                            slotIndex,
                            irIfThenElse(
                                context.irBuiltIns.intType,
                                irChanged(irGet(loopVar)),
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
                skipPreamble.statements.add(irEndMovableGroup())

                // if (dirty and 0b0110 === 0) {
                //   dirty = dirty or 0b0010
                // }
                skipPreamble.statements.add(
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
        parameters.forEach {
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
            bodyPreamble.statements.add(irStartDefaults(sourceElement))
            bodyPreamble.statements.add(
                irIfThenElse(
                    // this prevents us from re-executing the defaults if this function is getting
                    // executed from a recomposition
                    // if (%changed and 0b0001 === 0 || %composer.defaultsInvalid) {
                    condition = irOrOr(
                        irEqual(changedParam.irLowBit(), irConst(0)),
                        irDefaultsInvalid()
                    ),
                    // set all of the default temp vars
                    thenPart = setDefaults,
                    // composer.skipCurrentGroup()
                    elsePart = irBlock(
                        statements = listOf(
                            irSkipToGroupEnd(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
                            *skipDefaults.statements.toTypedArray()
                        )
                    )
                )
            )
            bodyPreamble.statements.add(irEndDefaults())
        }

        return mightSkip
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun irEndRestartGroupAndUpdateScope(
        scope: Scope.FunctionScope,
        changedParam: IrChangedBitMaskValue,
        defaultParam: IrDefaultBitMaskValue?,
        numRealValueParameters: Int
    ): IrExpression {
        val function = scope.function

        // Save the dispatch receiver into a temporary created in
        // the outer scope because direct references to the
        // receiver sometimes cause an invalid name, "$<this>", to
        // be generated.
        val dispatchReceiverParameter = function.dispatchReceiverParameter
        val outerReceiver = if (dispatchReceiverParameter != null) irTemporary(
            value = irGet(dispatchReceiverParameter),
            nameHint = "rcvr"
        ) else null

        // Create self-invoke lambda
        val lambdaDescriptor = AnonymousFunctionDescriptor(
            function.descriptor,
            Annotations.EMPTY,
            CallableMemberDescriptor.Kind.DECLARATION,
            SourceElement.NO_SOURCE,
            false
        )

        val passedInComposerParameter = ValueParameterDescriptorImpl(
            containingDeclaration = lambdaDescriptor,
            original = null,
            index = 0,
            annotations = Annotations.EMPTY,
            name = KtxNameConventions.COMPOSER_PARAMETER,
            outType = composerIrClass.defaultType.makeNullable().toKotlinType(),
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
        )

        val ignoredChangedParameter = ValueParameterDescriptorImpl(
            containingDeclaration = lambdaDescriptor,
            original = null,
            index = 1,
            annotations = Annotations.EMPTY,
            name = KtxNameConventions.CHANGED_PARAMETER,
            outType = builtIns.intType.toKotlinType(),
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false,
            varargElementType = null,
            source = SourceElement.NO_SOURCE
        )

        lambdaDescriptor.apply {
            initialize(
                null,
                null,
                emptyList(),
                listOf(passedInComposerParameter, ignoredChangedParameter),
                updateScopeBlockType.toKotlinType(),
                Modality.FINAL,
                DescriptorVisibilities.LOCAL
            )
        }

        val parameterCount = function.valueParameters.size
        val contextParameterCount = function.contextReceiverParametersCount
        val composerIndex = contextParameterCount + numRealValueParameters
        val changedIndex = composerIndex + 1
        val defaultIndex = changedIndex + changedParamCount(
            numRealValueParameters,
            function.thisParamCount
        )

        if (defaultParam == null) {
            require(parameterCount == defaultIndex) // param count is 1-based, index is 0-based
        } else {
            require(
                parameterCount == defaultIndex +
                    defaultParamCount(contextParameterCount + numRealValueParameters)
            )
        }

        val lambda = IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            IrSimpleFunctionSymbolImpl(lambdaDescriptor),
            name = lambdaDescriptor.name,
            visibility = lambdaDescriptor.visibility,
            modality = lambdaDescriptor.modality,
            returnType = context.irBuiltIns.unitType,
            isInline = lambdaDescriptor.isInline,
            isExternal = lambdaDescriptor.isExternal,
            isTailrec = lambdaDescriptor.isTailrec,
            isSuspend = lambdaDescriptor.isSuspend,
            isOperator = lambdaDescriptor.isOperator,
            isExpect = lambdaDescriptor.isExpect,
            isInfix = lambdaDescriptor.isInfix,
        ).also { fn ->
            fn.parent = function
            val localIrBuilder = DeclarationIrBuilder(context, fn.symbol)
            fn.addValueParameter(
                KtxNameConventions.COMPOSER_PARAMETER.identifier,
                composerIrClass.defaultType
                    .replaceArgumentsWithStarProjections()
                    .makeNullable()
            )
            fn.addValueParameter(
                "\$force",
                context.irBuiltIns.intType
            )
            fn.body = localIrBuilder.irBlockBody {
                // Call the function again with the same parameters
                +irReturn(
                    irCall(function.symbol).apply {
                        symbol.owner
                            .valueParameters
                            .forEachIndexed { index, param ->
                                if (param.isVararg) {
                                    putValueArgument(
                                        index,
                                        IrVarargImpl(
                                            UNDEFINED_OFFSET,
                                            UNDEFINED_OFFSET,
                                            param.type,
                                            param.varargElementType!!,
                                            elements = listOf(
                                                IrSpreadElementImpl(
                                                    UNDEFINED_OFFSET,
                                                    UNDEFINED_OFFSET,
                                                    irGet(param)
                                                )
                                            )
                                        )
                                    )
                                } else {
                                    // NOTE(lmr): should we be using the parameter here, or the temporary
                                    // with the default value?
                                    putValueArgument(index, irGet(param))
                                }
                            }

                        // new composer
                        putValueArgument(
                            composerIndex,
                            irGet(fn.valueParameters[0])
                        )

                        // the call in updateScope needs to *always* have the low bit set to 1.
                        // This ensures that the body of the function is actually executed.
                        changedParam.putAsValueArgumentInWithLowBit(
                            this,
                            changedIndex,
                            lowBit = true
                        )

                        defaultParam?.putAsValueArgumentIn(this, defaultIndex)

                        extensionReceiver = function.extensionReceiverParameter?.let { irGet(it) }
                        dispatchReceiver = outerReceiver?.let { irGet(it) }
                        function.typeParameters.forEachIndexed { index, parameter ->
                            putTypeArgument(index, parameter.defaultType)
                        }
                    }
                )
            }
        }

        // $composer.endRestartGroup()?.updateScope { next -> TheFunction(..., next) }
        return irBlock(
            statements = listOfNotNull(
                outerReceiver,
                irSafeCall(
                    irEndRestartGroup(),
                    updateScopeFunction.symbol,
                    irLambda(lambda, updateScopeBlockType)
                ),
                irTraceEventEnd()
            )
        )
    }

    private fun irIsSkipping() =
        irMethodCall(irCurrentComposer(), isSkippingFunction.getter!!)
    private fun irDefaultsInvalid() =
        irMethodCall(irCurrentComposer(), defaultsInvalidFunction.getter!!)

    private fun irIsProvided(default: IrDefaultBitMaskValue, slot: Int) =
        irEqual(default.irIsolateBitAtIndex(slot), irConst(0))

    // %changed and 0b111 == 0
    private fun irIsUncertainAndStable(changed: IrChangedBitMaskValue, slot: Int) = irEqual(
        changed.irIsolateBitsAtSlot(slot, includeStableBit = true),
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

    private fun IrBody.asBodyAndResultVar(): Pair<IrContainerExpression, IrVariable?> {
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
            if (expr is IrReturn) {
                block.statements.pop()
                return if (expr.value.type.isUnitOrNullableUnit() ||
                    expr.value.type.isNothing() ||
                    expr.value.type.isNullableNothing()
                ) {
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
            is IrValueDeclaration -> {
                // these declarations do not create new "scopes", so we do nothing
                return super.visitDeclaration(declaration)
            }
            else -> error("Unhandled declaration! ${declaration::class.java.simpleName}")
        }
    }

    private fun nearestComposer(): IrValueParameter =
        currentScope.nearestComposer
            ?: error("Not in a composable function \n${printScopeStack()}")

    private fun irCurrentComposer(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrExpression {
        return IrGetValueImpl(
            startOffset,
            endOffset,
            nearestComposer().symbol
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrElement.sourceKey(): Int {
        var hash = currentFunctionScope
            .function
            .symbol
            .descriptor
            .fqNameSafe
            .toString()
            .hashCode()
        hash = 31 * hash + startOffset
        if (this is IrConst<*>) {
            // Disambiguate ?. clauses which become a "null" constant expression
            hash = 31 * hash + (this.value?.hashCode() ?: 1)
        }
        return hash
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun functionSourceKey(): Int {
        val fn = currentFunctionScope.function
        if (fn is IrSimpleFunction) {
            return fn.sourceKey()
        } else {
            error("expected simple function: ${fn::class}")
        }
    }

    private fun IrElement.irSourceKey(): IrConst<Int> {
        return IrConstImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.intType,
            IrConstKind.Int,
            sourceKey()
        )
    }

    private fun irFunctionSourceKey(): IrConst<Int> {
        return IrConstImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.intType,
            IrConstKind.Int,
            functionSourceKey()
        )
    }

    private fun irStartReplaceableGroup(
        element: IrElement,
        scope: Scope.BlockScope,
        key: IrExpression = element.irSourceKey(),
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrExpression {
        return irWithSourceInformation(
            irMethodCall(
                irCurrentComposer(startOffset, endOffset),
                startReplaceableFunction,
                startOffset,
                endOffset
            ).also {
                it.putValueArgument(0, key)
            },
            scope
        )
    }

    private fun irWithSourceInformation(
        startGroup: IrExpression,
        scope: Scope.BlockScope
    ): IrExpression {
        return if (scope.hasSourceInformation) {
            irBlock(statements = listOf(startGroup, irSourceInformation(scope)))
        } else startGroup
    }

    private fun irSourceInformation(scope: Scope.BlockScope): IrExpression {
        val sourceInformation = irCall(
            sourceInformationFunction
        ).also {
            it.putValueArgument(0, irCurrentComposer())
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
            it.putValueArgument(0, irCurrentComposer())
            it.putValueArgument(1, key)
            recordSourceParameter(it, 2, scope)
        }
    }

    private fun irIsTraceInProgress(): IrExpression? =
        isTraceInProgressFunction?.let { irCall(it) }

    private fun irIfTraceInProgress(body: IrExpression): IrExpression? =
        irIsTraceInProgress()?.let { isTraceInProgress ->
            irIf(isTraceInProgress, body)
        }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun irTraceEventStart(key: IrExpression, declaration: IrFunction): IrExpression? =
        traceEventStartFunction?.let { traceEventStart ->
            val startOffset = declaration.body!!.startOffset
            val endOffset = declaration.body!!.endOffset

            val name = declaration.kotlinFqName
            val file = declaration.file.name
            val line = declaration.file.fileEntry.getLineNumber(declaration.startOffset)
            val traceInfo = "$name ($file:$line)" // TODO(174715171) decide on what to log
            val dirty1 = irConst(-1) // placeholder TODO(228314276): implement
            val dirty2 = irConst(-1) // placeholder TODO(228314276): implement

            irIfTraceInProgress(
                irCall(traceEventStart, startOffset, endOffset).also {
                    it.putValueArgument(0, key)
                    it.putValueArgument(1, dirty1)
                    it.putValueArgument(2, dirty2)
                    it.putValueArgument(3, irConst(traceInfo))
                }
            )
        }

    private fun irTraceEventEnd(): IrExpression? =
        traceEventEndFunction?.let {
            irIfTraceInProgress(irCall(it))
        }

    private fun irSourceInformationMarkerEnd(
        element: IrElement,
    ): IrExpression {
        return irCall(
            sourceInformationMarkerEndFunction,
            element.startOffset,
            element.endOffset
        ).also {
            it.putValueArgument(0, irCurrentComposer())
        }
    }

    private fun irStartDefaults(element: IrElement): IrExpression {
        return irMethodCall(
            irCurrentComposer(),
            startDefaultsFunction,
            element.startOffset,
            element.endOffset
        )
    }

    private fun irStartRestartGroup(
        element: IrElement,
        scope: Scope.BlockScope,
        key: IrExpression = element.irSourceKey()
    ): IrExpression {
        return irWithSourceInformation(
            irSet(
                nearestComposer(),
                irMethodCall(
                    irCurrentComposer(),
                    startRestartGroupFunction,
                    element.startOffset,
                    element.endOffset
                ).also {
                    it.putValueArgument(0, key)
                }
            ),
            scope
        )
    }

    private fun irEndRestartGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endRestartGroupFunction)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun irCache(
        startOffset: Int,
        endOffset: Int,
        returnType: IrType,
        invalid: IrExpression,
        calculation: IrExpression
    ): IrExpression {
        val symbol = referenceFunction(cacheFunction.symbol)
        return IrCallImpl(
            startOffset,
            endOffset,
            returnType,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            symbol.owner.valueParameters.size
        ).apply {
            extensionReceiver = irCurrentComposer()
            putValueArgument(0, invalid)
            putValueArgument(1, calculation)
            putTypeArgument(0, returnType)
        }
    }

    private fun irChanged(value: IrExpression): IrExpression {
        // compose has a unique opportunity to avoid inline class boxing for changed calls, since
        // we know that the only thing that we are detecting here is "changed or not", we can
        // just as easily pass in the underlying value, which will avoid boxing to check for
        // equality on recompositions. As a result here we want to pass in the underlying
        // property value for inline classes, not the instance itself. The inline class lowering
        // will turn this into just passing the wrapped value later on. If the type is already
        // boxed, then we don't want to unnecessarily _unbox_ it. Note that if Kotlin allows for
        // an overridden equals method of inline classes in the future, we may have to avoid the
        // boxing in a different way.
        val type = value.type.unboxInlineClass()
        val expr = value.unboxValueIfInline()
        val descriptor = type
            .toPrimitiveType()
            .let { changedPrimitiveFunctions[it] } ?: changedFunction
        return irMethodCall(irCurrentComposer(), descriptor).also {
            it.putValueArgument(0, expr)
        }
    }

    private fun irSkipToGroupEnd(startOffset: Int, endOffset: Int): IrExpression {
        return irMethodCall(
            irCurrentComposer(startOffset, endOffset),
            skipToGroupEndFunction,
            startOffset,
            endOffset
        )
    }

    private fun irSkipCurrentGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), skipCurrentGroupFunction)
    }

    private fun irEndReplaceableGroup(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrExpression {
        return irMethodCall(
            irCurrentComposer(startOffset, endOffset),
            endReplaceableFunction,
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
        scope: Scope.BlockScope
    ): IrExpression {
        return irWithSourceInformation(
            irMethodCall(
                irCurrentComposer(),
                startMovableFunction,
                element.startOffset,
                element.endOffset
            ).also {
                it.putValueArgument(0, element.irSourceKey())
                it.putValueArgument(1, joinedData)
            },
            scope
        )
    }

    private fun irEndMovableGroup(): IrExpression {
        return irMethodCall(irCurrentComposer(), endMovableFunction)
    }

    private fun irJoinKeyChain(keyExprs: List<IrExpression>): IrExpression {
        return keyExprs.reduce { accumulator, value ->
            irMethodCall(irCurrentComposer(), joinKeyFunction).apply {
                putValueArgument(0, accumulator)
                putValueArgument(1, value)
            }
        }
    }

    private fun irSafeCall(
        target: IrExpression,
        symbol: IrFunctionSymbol,
        vararg args: IrExpression
    ): IrExpression {
        val tmpVal = irTemporary(target, nameHint = "safe_receiver")
        return irBlock(
            origin = IrStatementOrigin.SAFE_CALL,
            statements = listOf(
                tmpVal,
                irIfThenElse(
                    condition = irEqual(irGet(tmpVal), irNull()),
                    thenPart = irNull(),
                    elsePart = irCall(symbol).apply {
                        dispatchReceiver = irGet(tmpVal)
                        args.forEachIndexed { i, arg ->
                            putValueArgument(i, arg)
                        }
                    }
                )
            )
        )
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun irCall(
        function: IrFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrCall {
        val type = function.returnType
        val symbol = referenceFunction(function.symbol)
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            symbol as IrSimpleFunctionSymbol,
            symbol.owner.typeParameters.size,
            symbol.owner.valueParameters.size
        )
    }

    private fun irMethodCall(
        target: IrExpression,
        function: IrFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET
    ): IrCall {
        return irCall(function, startOffset, endOffset).apply {
            dispatchReceiver = target
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun irTemporary(
        value: IrExpression,
        nameHint: String? = null,
        irType: IrType = value.type,
        isVar: Boolean = false,
        exactName: Boolean = false
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
        )
    }

    private fun IrBlock.withReplaceableGroupStatements(scope: Scope.BlockScope): IrExpression {
        currentFunctionScope.metrics.recordGroup()
        scope.realizeGroup(::irEndReplaceableGroup)
        return when {
            // if the scope ends with a return call, then it will get properly ended if we
            // just push the end call on the scope because of the way returns get transformed in
            // this class. As a result, here we can safely just "prepend" the start call
            endsWithReturnOrJump() -> IrBlockImpl(
                startOffset,
                endOffset,
                type,
                origin,
                listOf(irStartReplaceableGroup(this, scope)) + statements
            )
            // otherwise, we want to push an end call for any early returns/jumps, but also add
            // an end call to the end of the group
            else -> IrBlockImpl(
                startOffset,
                endOffset,
                type,
                origin,
                listOf(
                    irStartReplaceableGroup(
                        this,
                        scope,
                        startOffset = startOffset,
                        endOffset = endOffset
                    )
                ) + statements + listOf(irEndReplaceableGroup(startOffset, endOffset))
            )
        }
    }

    private fun IrExpression.asReplaceableGroup(scope: Scope.BlockScope): IrExpression {
        currentFunctionScope.metrics.recordGroup()
        // if the scope has no composable calls, then the only important thing is that a
        // start/end call gets executed. as a result, we can just put them both at the top of
        // the group, and we don't have to deal with any of the complicated jump logic that
        // could be inside of the block
        if (!scope.hasComposableCalls && !scope.hasReturn && !scope.hasJump) {
            return wrap(
                before = listOf(
                    irStartReplaceableGroup(
                        this,
                        scope,
                        startOffset = startOffset,
                        endOffset = endOffset,
                    ),
                    irEndReplaceableGroup(startOffset, endOffset)
                )
            )
        }
        scope.realizeGroup(::irEndReplaceableGroup)
        return when {
            // if the scope ends with a return call, then it will get properly ended if we
            // just push the end call on the scope because of the way returns get transformed in
            // this class. As a result, here we can safely just "prepend" the start call
            endsWithReturnOrJump() -> {
                wrap(before = listOf(irStartReplaceableGroup(this, scope)))
            }
            // otherwise, we want to push an end call for any early returns/jumps, but also add
            // an end call to the end of the group
            else -> {
                wrap(
                    before = listOf(
                        irStartReplaceableGroup(
                            this,
                            scope,
                            startOffset = startOffset,
                            endOffset = endOffset
                        )
                    ),
                    after = listOf(irEndReplaceableGroup(startOffset, endOffset))
                )
            }
        }
    }

    private fun IrExpression.wrap(
        before: List<IrExpression> = emptyList(),
        after: List<IrExpression> = emptyList()
    ): IrExpression {
        return if (after.isEmpty() || type.isNothing() || type.isUnit()) {
            wrap(type, before, after)
        } else {
            val tmpVar = irTemporary(this, nameHint = "group")
            tmpVar.wrap(
                type,
                before,
                after + irGet(tmpVar)
            )
        }
    }

    private fun IrStatement.wrap(
        type: IrType,
        before: List<IrExpression> = emptyList(),
        after: List<IrExpression> = emptyList()
    ): IrExpression {
        return IrBlockImpl(
            startOffset,
            endOffset,
            type,
            null,
            before + this + after
        )
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
                    before.statements.add(irStartReplaceableGroup(this, scope))
                    after.statements.add(irEndReplaceableGroup())
                }
            },
            makeEnd = ::irEndReplaceableGroup
        )
        return wrap(
            listOf(before),
            listOf(after)
        )
    }

    private fun mutableStatementContainer(): IrContainerExpression {
        // NOTE(lmr): It's important to use IrComposite here so that we don't introduce any new
        // scopes
        return IrCompositeImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            context.irBuiltIns.unitType
        )
    }

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
        makeEnd: () -> IrExpression
    ) {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    scope.markCoalescableGroup(coalescableScope, realizeGroup, makeEnd)
                    if (!scope.isInlinedLambda) {
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
        extraEndLocation: (IrExpression) -> Unit
    ) {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.function == symbol.owner) {
                        scope.markReturn(extraEndLocation)
                        break@loop
                    }
                }
                is Scope.BlockScope -> {
                    scope.markReturn(extraEndLocation)
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

    data class ParamMeta(
        var stability: Stability = Stability.Unstable,
        var isVararg: Boolean = false,
        var isProvided: Boolean = false,
        var isStatic: Boolean = false,
        var isCertain: Boolean = false,
        var maskSlot: Int = -1,
        var maskParam: IrChangedBitMaskValue? = null
    )

    fun paramMetaOf(arg: IrExpression, isProvided: Boolean): ParamMeta {
        val meta = ParamMeta(isProvided = isProvided)
        populateParamMeta(arg, meta)
        return meta
    }

    private fun populateParamMeta(arg: IrExpression, meta: ParamMeta) {

        meta.stability = stabilityOf(arg)
        when {
            arg.isStatic() -> meta.isStatic = true
            arg is IrGetValue -> {
                val owner = arg.symbol.owner
                when (owner) {
                    is IrValueParameter -> {
                        extractParamMetaFromScopes(meta, owner)
                    }
                    is IrVariable -> {
                        if (owner.isConst) {
                            meta.isStatic = true
                        } else if (!owner.isVar && owner.initializer != null) {
                            populateParamMeta(owner.initializer!!, meta)
                        }
                    }
                }
            }
        }
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

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.isTransformedComposableCall() || expression.isSyntheticComposableCall()) {
            return visitComposableCall(expression)
        }
        when {
            expression.symbol.owner.isInline -> {
                // if it is not a composable call but it is an inline function, then we allow
                // composable calls to happen inside of the inlined lambdas. This means that we have
                // some control flow analysis to handle there as well. We wrap the call in a
                // CallScope and coalescable group if the call has any composable invocations inside
                // of it..
                val captureScope = withScope(Scope.CaptureScope()) {
                    expression.transformChildrenVoid()
                }
                return if (captureScope.hasCapturedComposableCall) {
                    expression.asCoalescableGroup(captureScope)
                } else {
                    expression
                }
            }
            expression.isComposableSingletonGetter() -> {
                // This looks like `ComposableSingletonClass.lambda-123`, which is a static/saved
                // call of composableLambdaInstance. We want to transform the property here now
                // so the assuptions about the invocation order assumed by source locations is
                // preserved.
                val getter = expression.symbol.owner
                val property = getter.correspondingPropertySymbol?.owner
                property?.transformChildrenVoid()
                return super.visitCall(expression)
            }
            else -> return super.visitCall(expression)
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun visitComposableCall(expression: IrCall): IrExpression {
        return when (expression.symbol.descriptor.fqNameSafe) {
            ComposeFqNames.remember -> {
                if (intrinsicRememberEnabled) {
                    visitRememberCall(expression)
                } else {
                    visitNormalComposableCall(expression)
                }
            }
            ComposeFqNames.key -> visitKeyCall(expression)
            DecoyFqNames.key -> visitKeyCall(expression)
            else -> visitNormalComposableCall(expression)
        }
    }

    private fun visitNormalComposableCall(expression: IrCall): IrExpression {
        encounteredComposableCall(
            withGroups = !expression.symbol.owner.isReadonly()
        )
        // it's important that we transform all of the parameters here since this will cause the
        // IrGetValue's of remapped default parameters to point to the right variable.
        expression.transformChildrenVoid()

        val ownerFn = expression.symbol.owner
        val numValueParams = ownerFn.valueParameters.size
        val numContextParams = ownerFn.contextReceiverParametersCount
        val numDefaults: Int
        val numChanged: Int
        val numRealValueParams: Int

        val hasDefaults = ownerFn.valueParameters.any {
            it.name == KtxNameConventions.DEFAULT_PARAMETER
        }
        if (!hasDefaults && expression.isInvoke()) {
            // in the case of an invoke without any defaults, all of the parameters are going to
            // be type parameter args which won't have special names.
            // In this case, we know that the values cannot
            // be defaulted though, so we can calculate the number of real parameters based on
            // the total number of parameters
            numDefaults = 0
            numChanged = changedParamCountFromTotal(
                // Subtracting context params from total since they are included in thisParams
                numValueParams - numContextParams + ownerFn.thisParamCount
            )
            numRealValueParams = numValueParams -
                numContextParams -
                1 - // composer param
                numChanged
        } else {
            // Context receiver params are value parameters and will precede real params, calculate
            // the amount of real params by finding the index off the last real param (if any) and
            // offsetting it by the amount of context receiver params.
            val indexOfLastRealParam = ownerFn.valueParameters.indexOfLast {
                !it.name.asString().startsWith('$')
            }
            numRealValueParams = if (indexOfLastRealParam != -1) {
                (indexOfLastRealParam + 1) - numContextParams
            } else {
                0
            }
            numDefaults = if (hasDefaults) {
                defaultParamCount(numContextParams + numRealValueParams)
            } else {
                0
            }
            numChanged = changedParamCount(numRealValueParams, ownerFn.thisParamCount)
        }

        require(
            numContextParams +
            numRealValueParams +
                1 + // composer param
                numChanged +
                numDefaults == numValueParams
        )

        val composerIndex = numContextParams + numRealValueParams
        val changedArgIndex = composerIndex + 1
        val defaultArgIndex = changedArgIndex + numChanged
        val defaultArgs = (defaultArgIndex until numValueParams).map {
            expression.getValueArgument(it)
        }
        val hasDefaultArgs = defaultArgs.isNotEmpty()

        val defaultMasks = defaultArgs.map {
            when (it) {
                !is IrConst<*> -> error("Expected default mask to be a const")
                else -> it.value as? Int ?: error("Expected default mask to be an Int")
            }
        }

        val contextMeta = mutableListOf<ParamMeta>()
        val paramMeta = mutableListOf<ParamMeta>()

        for (index in 0 until numContextParams + numRealValueParams) {
            val arg = expression.getValueArgument(index)
            if (arg == null) {
                val param = expression.symbol.owner.valueParameters[index]
                if (param.varargElementType == null) {
                    // ComposerParamTransformer should not allow for any null arguments on a composable
                    // invocation unless the parameter is vararg. If this is null here, we have
                    // missed something.
                    error("Unexpected null argument for composable call")
                } else {
                    paramMeta.add(ParamMeta(isVararg = true))
                    continue
                }
            }
            if (index < numContextParams) {
                val meta = paramMetaOf(arg, isProvided = true)
                contextMeta.add(meta)
            } else {
                val bitIndex = defaultsBitIndex(index)
                val maskValue = if (hasDefaultArgs) defaultMasks[defaultsParamIndex(index)] else 0
                val meta = paramMetaOf(arg, isProvided = maskValue and (0b1 shl bitIndex) == 0)
                paramMeta.add(meta)
            }
        }

        val extensionMeta = expression.extensionReceiver?.let { paramMetaOf(it, isProvided = true) }
        val dispatchMeta = expression.dispatchReceiver?.let { paramMetaOf(it, isProvided = true) }

        val changedParams = buildChangedParamsForCall(
            contextParams = contextMeta,
            valueParams = paramMeta,
            extensionParam = extensionMeta,
            dispatchParam = dispatchMeta
        )

        changedParams.forEachIndexed { i, param ->
            expression.putValueArgument(changedArgIndex + i, param)
        }

        currentFunctionScope.metrics.recordComposableCall(
            expression,
            paramMeta
        )
        metrics.recordComposableCall(
            expression,
            paramMeta
        )
        recordCallInSource(call = expression)

        return expression
    }

    private fun canElideRememberGroup(): Boolean {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    return if (scope.hasComposableCallsWithGroups || scope.hasDefaultsGroup) {
                        false
                    } else if (scope.isInlinedLambda) {
                        scope = scope.parent
                        continue@loop
                    } else {
                        true
                    }
                }
                is Scope.BranchScope -> {
                    return !scope.hasComposableCallsWithGroups
                }
                else -> {
                    // Any other scope type the behavior is undefined and we cannot rely on
                    // intrinsic behavior
                    return false
                }
            }
        }
        return false
    }

    private fun visitRememberCall(expression: IrCall): IrExpression {
        val inputArgs = mutableListOf<IrExpression>()
        var hasSpreadArgs = false
        var calculationArg: IrExpression? = null
        for (i in 0 until expression.valueArgumentsCount) {
            val param = expression.symbol.owner.valueParameters[i]
            val arg = expression.getValueArgument(i)
                ?: error("Unexpected null argument found on key call")
            if (param.name.asString().startsWith('$'))
            // we are done. synthetic args go at
            // the end
                break

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

        if (calculationArg == null) {
            encounteredComposableCall(withGroups = true)
            recordCallInSource(call = expression)
            return expression
        }
        if (hasSpreadArgs || !canElideRememberGroup()) {
            encounteredComposableCall(withGroups = true)
            recordCallInSource(call = expression)
            calculationArg.transform(this, null)
            return expression
        }

        encounteredComposableCall(withGroups = false)

        val invalidExpr = inputArgs
            .mapNotNull(::irChangedOrInferredChanged)
            .reduceOrNull { acc, changed -> irBooleanOr(acc, changed) }
            ?: irConst(false)

        return irCache(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            invalidExpr,
            calculationArg.transform(this, null)
        )
    }

    private fun irChangedOrInferredChanged(arg: IrExpression): IrExpression? {
        val meta = paramMetaOf(arg, isProvided = true)
        val param = meta.maskParam

        return when {
            meta.isStatic -> null
            meta.isCertain &&
                meta.stability.knownStable() &&
                param is IrChangedBitMaskVariable -> {
                // if it's a dirty flag, and the parameter is _guaranteed_ to be stable, then we
                // know that the value is now CERTAIN, thus we can avoid calling changed completely
                //
                // invalid = invalid or (mask == different)
                irEqual(
                    param.irIsolateBitsAtSlot(meta.maskSlot, includeStableBit = true),
                    irConst(ParamState.Different.bitsForSlot(meta.maskSlot))
                )
            }
            meta.isCertain &&
                !meta.stability.knownUnstable() &&
                param is IrChangedBitMaskVariable -> {
                // if it's a dirty flag, and the parameter might be stable, then we only check
                // changed if the value is unstable, otherwise we can just check to see if the mask
                // is different
                //
                // invalid = invalid or (stable && mask == different || unstable && changed)

                val maskIsStableAndDifferent = irEqual(
                    param.irIsolateBitsAtSlot(meta.maskSlot, includeStableBit = true),
                    irConst(ParamState.Different.bitsForSlot(meta.maskSlot))
                )
                val stableBits = param.irSlotAnd(meta.maskSlot, StabilityBits.UNSTABLE.bits)
                val maskIsUnstableAndChanged = irAndAnd(
                    irNotEqual(stableBits, irConst(0)),
                    irChanged(arg)
                )
                irOrOr(
                    maskIsStableAndDifferent,
                    maskIsUnstableAndChanged
                )
            }
            meta.isCertain &&
                !meta.stability.knownUnstable() &&
                param != null -> {
                // if it's a changed flag then uncertain is a possible value. If it is uncertain
                // OR unstable, then we need to call changed. If it is uncertain or unstable here
                // it will _always_ be uncertain or unstable here, so this is safe. If it is not
                // uncertain or unstable, we can just check to see if its different

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
                        irChanged(arg)
                    ),
                    irEqual(
                        param.irIsolateBitsAtSlot(meta.maskSlot, includeStableBit = false),
                        irConst(ParamState.Different.bitsForSlot(meta.maskSlot))
                    )
                )
            }
            else -> irChanged(arg)
        }
    }

    private fun visitKeyCall(expression: IrCall): IrExpression {
        encounteredComposableCall(withGroups = true)
        val keyArgs = mutableListOf<IrExpression>()
        var blockArg: IrExpression? = null
        for (i in 0 until expression.valueArgumentsCount) {
            val param = expression.symbol.owner.valueParameters[i]
            val arg = expression.getValueArgument(i)
                ?: error("Unexpected null argument found on key call")
            if (param.name.asString().startsWith('$'))
            // we are done. synthetic args go at
            // the end
                break

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
            error("Expected function expression but was ${blockArg?.let{it::class}}")

        val (block, resultVar) = blockArg.function.body!!.asBodyAndResultVar()

        var transformed: IrExpression = block

        val scope = withScope(Scope.BranchScope()) {
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
                if (
                    value is IrValueParameter && value.name == KtxNameConventions.COMPOSER_PARAMETER
                ) {
                    return irCurrentComposer()
                } else {
                    return expression
                }
            }
        })

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
                irEndMovableGroup(),
                after,
                resultVar?.let { irGet(resultVar) }
            )
        )
    }

    private fun extractParamMetaFromScopes(meta: ParamMeta, param: IrValueDeclaration): Boolean {
        var scope: Scope? = currentScope
        val fn = param.parent
        while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.function == fn) {
                        if (scope.isComposable) {
                            val slotIndex = scope.allTrackedParams.indexOf(param)
                            if (slotIndex != -1) {
                                meta.isCertain = true
                                meta.maskParam = scope.dirty
                                meta.maskSlot = slotIndex
                            }
                        }
                        return true
                    }
                }
                else -> {
                    /* Do nothing, continue traversing */
                }
            }
            scope = scope.parent
        }
        return false
    }

    private fun buildChangedParamsForCall(
        contextParams: List<ParamMeta>,
        valueParams: List<ParamMeta>,
        extensionParam: ParamMeta?,
        dispatchParam: ParamMeta?
    ): List<IrExpression> {
        val allParams = listOfNotNull(extensionParam) +
            contextParams +
            valueParams +
            listOfNotNull(dispatchParam)
        // passing in 0 for thisParams since they should be included in the params list
        val changedCount = changedParamCount(allParams.size, 0)
        val result = mutableListOf<IrExpression>()
        for (i in 0 until changedCount) {
            val start = i * SLOTS_PER_INT
            val end = min(start + SLOTS_PER_INT, allParams.size)
            val slice = allParams.subList(start, end)
            result.add(buildChangedParamForCall(slice))
        }
        return result
    }

    private fun buildChangedParamForCall(params: List<ParamMeta>): IrExpression {
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

        params.forEachIndexed { slot, meta ->
            val stability = meta.stability
            when {
                stability.knownUnstable() -> {
                    bitMaskConstant = bitMaskConstant or StabilityBits.UNSTABLE.bitsForSlot(slot)
                    // If it is known to be unstable, there's no purpose in propagating any
                    // additional metadata _for this parameter_, but we still want to propagate
                    // the other parameters.
                    return@forEachIndexed
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
                            val int = context.irBuiltIns.intType
                            val bitsToShiftLeft = slot * BITS_PER_SLOT

                            irCall(
                                int.binaryOperator(
                                    OperatorNameConventions.SHL,
                                    int
                                ),
                                null,
                                it,
                                null,
                                irConst(bitsToShiftLeft)
                            )
                        }
                        orExprs.add(expr)
                    }
                }
            }
            if (meta.isVararg) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else if (!meta.isProvided) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else if (meta.isStatic) {
                bitMaskConstant = bitMaskConstant or ParamState.Static.bitsForSlot(slot)
            } else if (!meta.isCertain) {
                bitMaskConstant = bitMaskConstant or ParamState.Uncertain.bitsForSlot(slot)
            } else {
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

    fun irTypeParameterStability(param: IrTypeParameter): IrExpression? {
        var scope: Scope? = currentScope
        loop@ while (scope != null) {
            when (scope) {
                is Scope.FunctionScope -> {
                    if (scope.isComposable) {
                        val fn = scope.function
                        val maskParam = scope.dirty ?: scope.changedParameter
                        if (maskParam != null && fn.typeParameters.isNotEmpty()) {
                            for (it in fn.valueParameters) {
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
                is Scope.ClassScope -> {
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
        if (!isInComposableScope) return super.visitReturn(expression)
        expression.transformChildrenVoid()
        val endBlock = mutableStatementContainer()
        encounteredReturn(expression.returnTargetSymbol) { endBlock.statements.add(it) }
        return if (expression.value.type
            .also { if (it is IrSimpleType) it.classifier }
            .isUnitOrNullableUnit()
        ) {
            expression.wrap(listOf(endBlock))
        } else {
            val tempVar = irTemporary(expression.value, nameHint = "return")
            tempVar.wrap(
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
                loop.condition = loop.condition.asReplaceableGroup(loopScope)
            }

            loop.body = loop.body?.transform(this, null)
            if (loopScope.needsGroupPerIteration && loopScope.hasComposableCalls) {
                val current = loop.body
                if (current is IrBlock) {
                    loop.body = current.withReplaceableGroupStatements(loopScope)
                } else {
                    loop.body = current?.asReplaceableGroup(loopScope)
                }
            }
        }
        return if (!loopScope.needsGroupPerIteration && loopScope.hasComposableCalls) {
            loop.asCoalescableGroup(loopScope)
        } else {
            loop
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        if (!isInComposableScope) return super.visitWhen(expression)

        // Composable calls in conditions are more expensive than composable calls in the different
        // result branches of the when clause. This is because if we have N branches of a when
        // clause, we will always execute exactly 1 result branch, but we will execute 0-N of the
        // conditions. This means that if only the results have composable calls, we can use
        // replaceable groups to represent the entire expression. If a condition has a composable
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
            expression.branches.forEachIndexed { index, it ->
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
                    // it doesn't necessitate a group
                    needsWrappingGroup =
                        needsWrappingGroup || (index != 0 && condScope.hasComposableCalls)
                    if (resultScope.hasComposableCalls)
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

        // If we are putting groups around the result branches, we need to guarantee that exactly
        // one result branch is executed. We do this by adding an else branch if it there is not
        // one already. Note that we only need to do this if we aren't going to wrap the if
        // statement in a group entirely, which we will do if the conditions have calls in them.
        // NOTE: we might also be able to assume that the when is exhaustive if it has a non-unit
        // resulting type, since the type system should enforce that.
        if (!hasElseBranch && resultsWithCalls > 1 && !needsWrappingGroup) {
            condScopes.add(Scope.BranchScope())
            resultScopes.add(Scope.BranchScope())
            transformed.branches.add(
                IrElseBranchImpl(
                    expression.endOffset,
                    expression.endOffset,
                    condition = IrConstImpl(
                        expression.endOffset,
                        expression.endOffset,
                        context.irBuiltIns.booleanType,
                        IrConstKind.Boolean,
                        true
                    ),
                    result = IrBlockImpl(
                        expression.endOffset,
                        expression.endOffset,
                        context.irBuiltIns.unitType,
                        null,
                        emptyList()
                    )
                )
            )
        }

        forEachWith(transformed.branches, condScopes, resultScopes) { it, condScope, resultScope ->
            // If the conditional block doesn't have a composable call in it, we don't need
            // to generate a group around it because we will be generating one around the entire
            // if statement
            if (needsWrappingGroup && condScope.hasComposableCalls) {
                it.condition = it.condition.asReplaceableGroup(condScope)
            }
            if (
                // if no wrapping group but more than one result have calls, we have to have every
                // result be a group so that we have a consistent number of groups during execution
                (resultsWithCalls > 1 && !needsWrappingGroup) ||
                // if we are wrapping the if with a group, then we only need to add a group when
                // the block has composable calls
                (needsWrappingGroup && resultScope.hasComposableCalls)
            ) {
                it.result = it.result.asReplaceableGroup(resultScope)
            }
        }

        return when {
            resultsWithCalls == 1 ->
                transformed.asCoalescableGroup(resultScopes.single { it.hasComposableCalls })
            needsWrappingGroup ->
                transformed.asCoalescableGroup(whenScope)
            else ->
                transformed
        }
    }

    sealed class Scope(val name: String) {
        var parent: Scope? = null
        var level: Int = 0

        open val isInComposable get() = false
        open val functionScope: FunctionScope? get() = parent?.functionScope
        open val fileScope: FileScope? get() = parent?.fileScope
        open val nearestComposer: IrValueParameter? get() = parent?.nearestComposer

        open class SourceLocation(val element: IrElement) {
            open val repeatable: Boolean
                get() = false
            var used = false
                private set
            fun markUsed() { used = true }
        }

        class RootScope : Scope("<root>")
        class FunctionScope(
            val function: IrFunction,
            private val transformer: ComposableFunctionBodyTransformer
        ) : BlockScope("fun ${function.name.asString()}") {
            val isInlinedLambda: Boolean
                get() = transformer.inlineLambdaInfo.isInlineLambda(function)

            val metrics: FunctionMetrics = transformer.metricsFor(function)

            private var lastTemporaryIndex: Int = 0

            private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

            override val isInComposable: Boolean
                get() = isComposable ||
                    transformer.inlineLambdaInfo.preservesComposableScope(function) &&
                    parent?.isInComposable == true

            override val functionScope: FunctionScope? get() = this
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
            private fun parameterInformation(): String {
                val builder = StringBuilder("P(")
                val parameters = function.valueParameters.filter {
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

                parameters.forEachIndexed { originalIndex, parameter ->
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

            override fun sourceLocationOf(call: IrElement): SourceLocation {
                val parent = parent
                return if (isInlinedLambda && parent is BlockScope)
                    parent.sourceLocationOf(call)
                else super.sourceLocationOf(call)
            }

            private fun callInformation(): String =
                if (!function.name.isSpecial())
                    "C(${function.name.asString()})"
                else "C"

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
                    }:${sourceFileInformation()}"
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
                for (param in function.valueParameters) {
                    val paramName = param.name.asString()
                    when {
                        !paramName.startsWith('$') -> realValueParamCount++
                        paramName == KtxNameConventions.COMPOSER_PARAMETER.identifier ->
                            composerParameter = param
                        paramName.startsWith(KtxNameConventions.DEFAULT_PARAMETER.identifier) ->
                            defaultParams += param
                        paramName.startsWith(KtxNameConventions.CHANGED_PARAMETER.identifier) ->
                            changedParams += param
                        paramName.startsWith("\$anonymous\$parameter") -> Unit
                        paramName.startsWith("\$name\$for\$destructuring") -> Unit
                        paramName.startsWith("\$noName_") -> Unit
                        else -> {
                            Unit
                        }
                    }
                }
                slotCount = realValueParamCount
                slotCount += function.contextReceiverParametersCount
                if (function.extensionReceiverParameter != null) slotCount++
                if (function.dispatchReceiverParameter != null) {
                    slotCount++
                } else if (function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA) {
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
                        function.contextReceiverParametersCount + realValueParamCount,
                    )
                } else {
                    null
                }
            }

            val isComposable = composerParameter != null

            val allTrackedParams = listOfNotNull(function.extensionReceiverParameter) +
                function.valueParameters.take(
                    function.contextReceiverParametersCount + realValueParamCount
                ) +
                listOfNotNull(function.dispatchReceiverParameter)

            fun defaultIndexForSlotIndex(index: Int): Int {
                return if (function.extensionReceiverParameter != null) index - 1 else index
            }

            val usedParams = BooleanArray(slotCount) { false }

            init {
                if (
                    isComposable &&
                    function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                ) {
                    // in the case of a composable lambda, we want to make sure the dispatch
                    // receiver is always marked as "used"
                    usedParams[slotCount - 1] = true
                }
            }

            fun getNameForTemporary(nameHint: String?): String {
                val index = nextTemporaryIndex()
                return if (nameHint != null) "tmp${index}_$nameHint" else "tmp$index"
            }

            private fun packageName(): String? {
                var parent = function.parent
                while (true) {
                    when (parent) {
                        is IrPackageFragment -> return parent.fqName.asString()
                        is IrDeclaration -> parent = parent.parent
                        else -> break
                    }
                }
                return null
            }

            private fun packageHash(): Int =
                packageName()?.fold(0) { hash, current ->
                    hash * 31 + current.toInt()
                }?.absoluteValue ?: 0

            internal fun sourceFileInformation(): String {
                val hash = packageHash()
                if (hash != 0)
                    return "${function.file.name}#${hash.toString(36)}"
                return function.file.name
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
                if (coalescableChilds.isNotEmpty()) {
                    // if a call happens after the coalescable child group, then we should
                    // realize the group of the coalescable child
                    coalescableChilds.last().shouldRealize = true
                }
            }

            fun recordSourceLocation(call: IrElement, location: SourceLocation?): SourceLocation? {
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
                makeEnd: () -> IrExpression
            ) {
                addProvisionalSourceLocations(scope.sourceLocations)
                coalescableChilds.add(
                    CoalescableGroupInfo(
                        scope,
                        realizeGroup,
                        makeEnd
                    )
                )
            }

            open fun calculateHasSourceInformation(sourceInformationEnabled: Boolean): Boolean =
                sourceInformationEnabled && sourceLocations.isNotEmpty()

            open fun calculateSourceInfo(sourceInformationEnabled: Boolean): String? {
                return if (sourceInformationEnabled && sourceLocations.isNotEmpty()) {
                    val locations = sourceLocations
                        .filter { !it.used }
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
                coalescableChilds.forEach {
                    it.realize()
                }
            }

            open fun realizeEndCalls(makeEnd: () -> IrExpression) {
                extraEndLocations.forEach {
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
            private var coalescableChilds = mutableListOf<CoalescableGroupInfo>()

            class CoalescableGroupInfo(
                private val scope: BlockScope,
                private val realizeGroup: () -> Unit,
                private val makeEnd: () -> IrExpression
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
            override val fileScope: FileScope? get() = this
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
                    jumpEndLocations.forEach {
                        it(makeEnd())
                    }
                }
            }
        }
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
        class ComposableLambdaScope : BlockScope("composableLambda") {
            override fun calculateHasSourceInformation(sourceInformationEnabled: Boolean): Boolean {
                return sourceInformationEnabled
            }

            override fun calculateSourceInfo(sourceInformationEnabled: Boolean): String? =
                if (sourceInformationEnabled) {
                    "C${
                    super.calculateSourceInfo(sourceInformationEnabled) ?: ""
                    }:${functionScope?.sourceFileInformation() ?: ""}"
                } else {
                    null
                }
        }
    }

    inner class IrDefaultBitMaskValueImpl(
        private val params: List<IrValueParameter>,
        private val count: Int
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
            params.forEachIndexed { i, param ->
                fn.putValueArgument(
                    startIndex + i,
                    irGet(param)
                )
            }
        }
    }

    open inner class IrChangedBitMaskValueImpl(
        private val params: List<IrValueDeclaration>,
        private val count: Int
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

        override fun irSlotAnd(slot: Int, bits: Int): IrExpression {
            used = true
            // %changed and 0b11
            return irAnd(
                irGet(params[paramIndexForSlot(slot)]),
                irBitsForSlot(bits, slot)
            )
        }

        override fun irHasDifferences(usedParams: BooleanArray): IrExpression {
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
                val lhs = (start until end).fold(0) { mask, slot ->
                    if (usedParams[slot]) mask or bitsForSlot(0b101, slot) else mask
                }

                // we _only_ use this pattern for the slots where the body of the function
                // actually uses that parametser, otherwise we pass in 0b000 which will transfer
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
            exactName: Boolean
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
                    initializer = irGet(param)
                }
            }
            return IrChangedBitMaskVariableImpl(temps, count)
        }

        override fun putAsValueArgumentInWithLowBit(
            fn: IrFunctionAccessExpression,
            startIndex: Int,
            lowBit: Boolean
        ) {
            used = true
            params.forEachIndexed { index, param ->
                fn.putValueArgument(
                    startIndex + index,
                    if (index == 0) {
                        irOr(irGet(param), irConst(if (lowBit) 0b1 else 0b0))
                    } else {
                        irGet(param)
                    }
                )
            }
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
                if (bitsToShiftLeft > 0) shiftLeft else shiftRight,
                null,
                value,
                null,
                irConst(abs(bitsToShiftLeft))
            )
        }
    }

    inner class IrChangedBitMaskVariableImpl(
        private val temps: List<IrVariable>,
        count: Int
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
                    irInv(irConst(ParamState.Mask.bitsForSlot(slot)))
                )
            )
        }
    }
}

private fun String.replacePrefix(prefix: String, replacement: String) =
    if (startsWith(prefix)) replacement + substring(prefix.length) else this

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrFunction.isLambda(): Boolean {
    // There is probably a better way to determine this, but if there is, it isn't obvious
    return descriptor.name.asString() == "<anonymous>"
}

inline fun <A, B, C> forEachWith(a: List<A>, b: List<B>, c: List<C>, fn: (A, B, C) -> Unit) {
    for (i in a.indices) {
        fn(a[i], b[i], c[i])
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

private class GuardedLazy<out T>(initializer: () -> T) {
    private var _value: Any? = UNINITIALIZED_VALUE
    private var _initializer: (() -> T)? = initializer

    fun value(name: String): T {
        if (_value === UNINITIALIZED_VALUE) {
            try {
                _value = _initializer!!()
                _initializer = null
            } catch (e: Throwable) {
                throw java.lang.IllegalStateException("Error initializing $name", e)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return _value as T
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline operator fun <T> GuardedLazy<T>.getValue(thisRef: Any?, property: KProperty<*>) =
    value(property.name)

private fun <T> guardedLazy(initializer: () -> T) = GuardedLazy<T>(initializer)
