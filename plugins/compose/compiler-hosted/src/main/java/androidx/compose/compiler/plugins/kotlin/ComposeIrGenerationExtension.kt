/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.analysis.FqNameMatcher
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.lower.ClassStabilityTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposableFunInterfaceLowering
import androidx.compose.compiler.plugins.kotlin.lower.ComposableFunctionBodyTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposableLambdaAnnotator
import androidx.compose.compiler.plugins.kotlin.lower.ComposableSymbolRemapper
import androidx.compose.compiler.plugins.kotlin.lower.ComposableTargetAnnotationsTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposerIntrinsicTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposerLambdaMemoization
import androidx.compose.compiler.plugins.kotlin.lower.ComposerParamTransformer
import androidx.compose.compiler.plugins.kotlin.lower.CopyDefaultValuesFromExpectLowering
import androidx.compose.compiler.plugins.kotlin.lower.DurableFunctionKeyTransformer
import androidx.compose.compiler.plugins.kotlin.lower.DurableKeyVisitor
import androidx.compose.compiler.plugins.kotlin.lower.KlibAssignableParamTransformer
import androidx.compose.compiler.plugins.kotlin.lower.LiveLiteralTransformer
import androidx.compose.compiler.plugins.kotlin.lower.WrapJsComposableLambdaLowering
import androidx.compose.compiler.plugins.kotlin.lower.decoys.CreateDecoysTransformer
import androidx.compose.compiler.plugins.kotlin.lower.decoys.RecordDecoySignaturesTransformer
import androidx.compose.compiler.plugins.kotlin.lower.decoys.SubstituteDecoyCallsTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.DeclarationTable
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureFactory
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsGlobalDeclarationTable
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm

class ComposeIrGenerationExtension(
    @Suppress("unused") private val liveLiteralsEnabled: Boolean = false,
    @Suppress("unused") private val liveLiteralsV2Enabled: Boolean = false,
    private val generateFunctionKeyMetaClasses: Boolean = false,
    private val sourceInformationEnabled: Boolean = true,
    private val traceMarkersEnabled: Boolean = true,
    private val intrinsicRememberEnabled: Boolean = false,
    private val nonSkippingGroupOptimizationEnabled: Boolean = false,
    private val decoysEnabled: Boolean = false,
    private val metricsDestination: String? = null,
    private val reportsDestination: String? = null,
    private val validateIr: Boolean = false,
    private val useK2: Boolean = false,
    private val strongSkippingEnabled: Boolean = false,
    private val stableTypeMatchers: Set<FqNameMatcher> = emptySet(),
    private val moduleMetricsFactory: ((StabilityInferencer) -> ModuleMetrics)? = null
) : IrGenerationExtension {
    var metrics: ModuleMetrics = EmptyModuleMetrics
        private set

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        val isKlibTarget = !pluginContext.platform.isJvm()
        VersionChecker(pluginContext).check()

        val stabilityInferencer = StabilityInferencer(
            pluginContext.moduleDescriptor,
            stableTypeMatchers,
        )

        // Input check.  This should always pass, else something is horribly wrong upstream.
        // Necessary because oftentimes the issue is upstream (compiler bug, prior plugin, etc)
        if (validateIr)
            validateIr(moduleFragment, pluginContext.irBuiltIns)

        // create a symbol remapper to be used across all transforms
        val symbolRemapper = ComposableSymbolRemapper()

        if (useK2) {
            moduleFragment.acceptVoid(ComposableLambdaAnnotator(pluginContext))
        }

        if (moduleMetricsFactory != null) {
            metrics = moduleMetricsFactory.invoke(stabilityInferencer)
        } else if (metricsDestination != null || reportsDestination != null) {
            metrics = ModuleMetricsImpl(moduleFragment.name.asString()) {
                stabilityInferencer.stabilityOf(it)
            }
        }

        ClassStabilityTransformer(
            useK2,
            pluginContext,
            symbolRemapper,
            metrics,
            stabilityInferencer
        ).lower(moduleFragment)

        LiveLiteralTransformer(
            liveLiteralsEnabled || liveLiteralsV2Enabled,
            liveLiteralsV2Enabled,
            DurableKeyVisitor(),
            pluginContext,
            symbolRemapper,
            metrics,
            stabilityInferencer
        ).lower(moduleFragment)

        ComposableFunInterfaceLowering(pluginContext).lower(moduleFragment)

        val functionKeyTransformer = DurableFunctionKeyTransformer(
            pluginContext,
            symbolRemapper,
            metrics,
            stabilityInferencer
        )

        functionKeyTransformer.lower(moduleFragment)

        // Memoize normal lambdas and wrap composable lambdas
        ComposerLambdaMemoization(
            pluginContext,
            symbolRemapper,
            metrics,
            stabilityInferencer,
            strongSkippingEnabled,
            intrinsicRememberEnabled,
            nonSkippingGroupOptimizationEnabled,
        ).lower(moduleFragment)

        if (!useK2) {
            CopyDefaultValuesFromExpectLowering(pluginContext).lower(moduleFragment)
        }

        val mangler = when {
            pluginContext.platform.isJs() -> JsManglerIr
            else -> null
        }

        val idSignatureBuilder = when {
            pluginContext.platform.isJs() -> IdSignatureFactory(
                PublicIdSignatureComputer(mangler!!),
                DeclarationTable(JsGlobalDeclarationTable(pluginContext.irBuiltIns))
            )
            else -> null
        }
        if (decoysEnabled) {
            require(idSignatureBuilder != null) {
                "decoys are not supported for ${pluginContext.platform}"
            }

            CreateDecoysTransformer(
                pluginContext,
                symbolRemapper,
                idSignatureBuilder,
                stabilityInferencer,
                metrics,
            ).lower(moduleFragment)

            SubstituteDecoyCallsTransformer(
                pluginContext,
                symbolRemapper,
                idSignatureBuilder,
                stabilityInferencer,
                metrics,
            ).lower(moduleFragment)
        }

        // transform all composable functions to have an extra synthetic composer
        // parameter. this will also transform all types and calls to include the extra
        // parameter.
        ComposerParamTransformer(
            pluginContext,
            symbolRemapper,
            stabilityInferencer,
            decoysEnabled,
            metrics,
        ).lower(moduleFragment)

        ComposableTargetAnnotationsTransformer(
            pluginContext,
            symbolRemapper,
            metrics,
            stabilityInferencer
        ).lower(moduleFragment)

        // transform calls to the currentComposer to just use the local parameter from the
        // previous transform
        ComposerIntrinsicTransformer(pluginContext, decoysEnabled).lower(moduleFragment)

        ComposableFunctionBodyTransformer(
            pluginContext,
            symbolRemapper,
            metrics,
            stabilityInferencer,
            sourceInformationEnabled,
            traceMarkersEnabled,
            intrinsicRememberEnabled,
            nonSkippingGroupOptimizationEnabled,
            strongSkippingEnabled
        ).lower(moduleFragment)

        if (decoysEnabled) {
            require(idSignatureBuilder != null) {
                "decoys are not supported for ${pluginContext.platform}"
            }

            RecordDecoySignaturesTransformer(
                pluginContext,
                symbolRemapper,
                idSignatureBuilder,
                metrics,
                mangler!!,
                stabilityInferencer
            ).lower(moduleFragment)
        }

        if (isKlibTarget) {
            KlibAssignableParamTransformer(
                pluginContext,
                symbolRemapper,
                metrics,
                stabilityInferencer
            ).lower(moduleFragment)
        }

        if (pluginContext.platform.isJs()) {
            WrapJsComposableLambdaLowering(
                pluginContext,
                symbolRemapper,
                metrics,
                idSignatureBuilder!!,
                stabilityInferencer,
                decoysEnabled
            ).lower(moduleFragment)
        }

        if (generateFunctionKeyMetaClasses) {
            functionKeyTransformer.realizeKeyMetaAnnotations(moduleFragment)
        } else {
            functionKeyTransformer.removeKeyMetaClasses(moduleFragment)
        }

        if (metricsDestination != null) {
            metrics.saveMetricsTo(metricsDestination)
        }
        if (reportsDestination != null) {
            metrics.saveReportsTo(reportsDestination)
        }

        // Verify that our transformations didn't break something
        if (validateIr)
            validateIr(moduleFragment, pluginContext.irBuiltIns)
    }
}
