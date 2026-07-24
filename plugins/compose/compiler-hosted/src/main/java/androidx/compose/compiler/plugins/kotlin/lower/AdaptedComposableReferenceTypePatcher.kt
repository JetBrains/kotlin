/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

/**
 * Strips the `K` prefix from `KComposableFunctionN` static types of `IrFunctionReference` nodes
 * whose target will be lowered into an adapted function reference (i.e. the target has default
 * parameters — see `ComposerParamTransformer.adaptComposableReference`, useAdaptedOrigin=true).
 *
 * Why: for such refs, JVM's `FunctionReferenceLowering` picks `kotlin.jvm.internal.AdaptedFunctionReference`
 * as the runtime carrier (`FunctionReferenceLowering.isAdaptedReference` is true when the callee's
 * origin is `ADAPTER_FOR_CALLABLE_REFERENCE`). That carrier doesn't implement `kotlin.reflect.KFunction`.
 * `ComposableTypeRemapper` otherwise maps `KComposableFunctionN -> KFunctionN`, leaving the static
 * type promising reflection the runtime instance can't satisfy — and the resulting `CHECKCAST KFunction`
 * blows up at runtime.
 *
 * Rewriting the type to `@Composable FunctionN` here, before `ComposerLambdaMemoization` propagates it
 * into memoization wrappers (`remember<T>(…)` and friends), keeps the static type honest end-to-end:
 * the remapper later folds it into the plain `FunctionN<…, Composer, Int, …>` shape, which the runtime
 * `AdaptedFunctionReference` actually implements.
 *
 * Must run before [ComposerLambdaMemoization] so the memoization wrapper picks up the patched type.
 */
class AdaptedComposableReferenceTypePatcher(
    context: IrPluginContext,
    irModule: IrModuleFragment,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    featureFlags: FeatureFlags,
) : AbstractComposeLowering(context, irModule, metrics, stabilityInferencer, featureFlags),
    ModuleLoweringPass {

    override fun lower(irModule: IrModuleFragment) {
        irModule.transformChildrenVoid(this)
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        val type = expression.type as? IrSimpleType
        if (type != null && type.isKComposableFunction() && expression.targetWillBecomeAdaptedRef()) {
            expression.type = type.toComposableFunctionType()
        }
        return super.visitFunctionReference(expression)
    }

    private fun IrFunctionReference.targetWillBecomeAdaptedRef(): Boolean {
        // Mirrors the `useAdaptedOrigin = true` branch in ComposerParamTransformer.visitFunctionReference:
        // a target with default parameters gets wrapped in an adapter function with origin
        // ADAPTER_FOR_CALLABLE_REFERENCE — which the JVM backend materializes as AdaptedFunctionReference.
        val fn = symbol.owner as? IrSimpleFunction ?: return false
        return fn.parameters.any { it.defaultValue != null }
    }

    private fun IrSimpleType.toComposableFunctionType(): IrType {
        // Reuses the synthetic `ComposableFunctionN` class (non-K sibling of `KComposableFunctionN`,
        // same package). `ComposerParamTransformer.visitFunctionReference` still recognizes it via
        // `isSyntheticComposableFunction()` and runs adaptation; `ComposableTypeRemapper` then folds
        // it into plain `FunctionN<…, Composer, Int, …>`.
        val arity = arguments.size - /* return type */ 1
        val classId = ClassId(ComposeFqNames.InternalPackage, Name.identifier("ComposableFunction$arity"))
        val classifier = getTopLevelClass(classId)
        return IrSimpleTypeImpl(
            classifier = classifier,
            hasQuestionMark = isNullable(),
            arguments = arguments,
            annotations = annotations,
        )
    }
}
