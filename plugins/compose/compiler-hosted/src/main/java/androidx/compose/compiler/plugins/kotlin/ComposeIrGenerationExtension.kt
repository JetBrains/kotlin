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
import androidx.compose.compiler.plugins.kotlin.k1.ComposeDescriptorSerializerContext
import androidx.compose.compiler.plugins.kotlin.lower.*
import androidx.compose.compiler.plugins.kotlin.lower.hiddenfromobjc.AddHiddenFromObjCLowering
import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.validateIr
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.isWasm
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

class ComposeIrGenerationExtension(
    @Suppress("unused") private val liveLiteralsEnabled: Boolean = false,
    @Suppress("unused") private val liveLiteralsV2Enabled: Boolean = false,
    private val generateFunctionKeyMetaClasses: Boolean = false,
    private val sourceInformationEnabled: Boolean = true,
    private val traceMarkersEnabled: Boolean = true,
    private val metricsDestination: String? = null,
    private val reportsDestination: String? = null,
    private val irVerificationMode: IrVerificationMode = IrVerificationMode.NONE,
    private val useK2: Boolean = false,
    private val stableTypeMatchers: Set<FqNameMatcher> = emptySet(),
    private val moduleMetricsFactory: ((StabilityInferencer, FeatureFlags) -> ModuleMetrics)? = null,
    private val descriptorSerializerContext: ComposeDescriptorSerializerContext? = null,
    private val featureFlags: FeatureFlags,
    private val skipIfRuntimeNotFound: Boolean = false,
    private val messageCollector: MessageCollector,
) : IrGenerationExtension {
    var metrics: ModuleMetrics = EmptyModuleMetrics
        private set

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        val isKlibTarget = !pluginContext.platform.isJvm()
        if (VersionChecker(pluginContext, messageCollector).check(skipIfRuntimeNotFound) == VersionCheckerResult.NOT_FOUND) {
            return
        }

        val stabilityInferencer = StabilityInferencer(
            pluginContext.moduleDescriptor,
            stableTypeMatchers,
        )

        val irValidatorConfig = IrValidatorConfig(
            checkProperties = true,
            checkTypes = false, // TODO: Re-enable checking types (KT-68663)
        )

        // Input check.  This should always pass, else something is horribly wrong upstream.
        // Necessary because oftentimes the issue is upstream (compiler bug, prior plugin, etc)
        validateIr(messageCollector, irVerificationMode) {
            performBasicIrValidation(
                moduleFragment,
                pluginContext.irBuiltIns,
                phaseName = "Before Compose Compiler Plugin",
                irValidatorConfig,
            )
        }

        if (useK2) {
            moduleFragment.acceptVoid(ComposableLambdaAnnotator(pluginContext))
        }

        if (moduleMetricsFactory != null) {
            metrics = moduleMetricsFactory.invoke(stabilityInferencer, featureFlags)
        } else if (metricsDestination != null || reportsDestination != null) {
            metrics = ModuleMetricsImpl(moduleFragment.name.asString(), featureFlags) {
                stabilityInferencer.stabilityOf(it)
            }
        }

        if (pluginContext.platform.isNative()) {
            AddHiddenFromObjCLowering(
                pluginContext,
                metrics,
                descriptorSerializerContext?.hideFromObjCDeclarationsSet,
                stabilityInferencer,
                featureFlags,
            ).lower(moduleFragment)
        }

        ClassStabilityTransformer(
            useK2,
            pluginContext,
            metrics,
            stabilityInferencer,
            classStabilityInferredCollection = descriptorSerializerContext
                ?.classStabilityInferredCollection?.takeIf {
                    !pluginContext.platform.isJvm()
                },
            featureFlags,
            messageCollector
        ).lower(moduleFragment)

        if (liveLiteralsEnabled || liveLiteralsV2Enabled) {
            LiveLiteralTransformer(
                liveLiteralsEnabled = true,
                usePerFileEnabledFlag = liveLiteralsV2Enabled,
                keyVisitor = DurableKeyVisitor(),
                context = pluginContext,
                metrics = metrics,
                stabilityInferencer = stabilityInferencer,
                featureFlags = featureFlags,
            ).lower(moduleFragment)
        }

        ComposableFunInterfaceLowering(pluginContext).lower(moduleFragment)

        val functionKeyTransformer = DurableFunctionKeyTransformer(
            pluginContext,
            metrics,
            stabilityInferencer,
            featureFlags,
        )

        functionKeyTransformer.lower(moduleFragment)

        if (!useK2) {
            CopyDefaultValuesFromExpectLowering(pluginContext).lower(moduleFragment)
        }

        // Generate default wrappers for virtual functions
        ComposableDefaultParamLowering(
            pluginContext,
            metrics,
            stabilityInferencer,
            featureFlags
        ).lower(moduleFragment)

        // Memoize normal lambdas and wrap composable lambdas
        ComposerLambdaMemoization(
            pluginContext,
            metrics,
            stabilityInferencer,
            featureFlags,
        ).lower(moduleFragment)

        // transform all composable functions to have an extra synthetic composer
        // parameter. this will also transform all types and calls to include the extra
        // parameter.
        ComposerParamTransformer(
            pluginContext,
            stabilityInferencer,
            metrics,
            featureFlags,
        ).lower(moduleFragment)

        ComposableTargetAnnotationsTransformer(
            pluginContext,
            metrics,
            stabilityInferencer,
            featureFlags,
        ).lower(moduleFragment)

        // transform calls to the currentComposer to just use the local parameter from the
        // previous transform
        ComposerIntrinsicTransformer(pluginContext).lower(moduleFragment)

        ComposableFunctionBodyTransformer(
            pluginContext,
            metrics,
            stabilityInferencer,
            sourceInformationEnabled,
            traceMarkersEnabled,
            featureFlags,
        ).lower(moduleFragment)

        if (isKlibTarget) {
            KlibAssignableParamTransformer(
                pluginContext,
                metrics,
                stabilityInferencer,
                featureFlags,
            ).lower(moduleFragment)
        }

        if (pluginContext.platform.isJs() || pluginContext.platform.isWasm()) {
            WrapJsComposableLambdaLowering(
                pluginContext,
                metrics,
                stabilityInferencer,
                featureFlags,
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
        validateIr(messageCollector, irVerificationMode) {
            performBasicIrValidation(
                moduleFragment,
                pluginContext.irBuiltIns,
                phaseName = "After Compose Compiler Plugin",
                irValidatorConfig,
            )
        }
    }
}
