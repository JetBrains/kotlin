/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.compiler.plugins.kotlin.lower.decoys

import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
import androidx.compose.compiler.plugins.kotlin.lower.AbstractComposeLowering
import androidx.compose.compiler.plugins.kotlin.lower.containsComposableAnnotation
import androidx.compose.compiler.plugins.kotlin.lower.includeFileNameInExceptionTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureFactory
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.util.parentAsClass

abstract class AbstractDecoysLowering(
    pluginContext: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    override val signatureBuilder: IdSignatureFactory,
    featureFlags: FeatureFlags,
) : AbstractComposeLowering(
    context = pluginContext,
    symbolRemapper = symbolRemapper,
    metrics = metrics,
    stabilityInferencer = stabilityInferencer,
    featureFlags = featureFlags
), DecoyTransformBase {

    override fun visitFile(declaration: IrFile): IrFile {
        includeFileNameInExceptionTrace(declaration) {
            var file: IrFile = declaration
            // since kotlin 1.6.0-RC2 signatureBuilder needs to "know" fileSignature available
            // within inFile scope. It's necessary to ensure signatures calc for private top level
            // decoys.
            signatureBuilder.inFile(file = declaration.symbol) {
                file = super.visitFile(declaration)
            }
            return file
        }
    }

    protected fun IrFunction.shouldBeRemapped(): Boolean =
        !isLocalFunction() &&
            !isEnumConstructor() &&
            (hasComposableAnnotation() || hasComposableParameter())

    private fun IrFunction.isLocalFunction(): Boolean =
        origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA ||
            (isLocal && (this is IrSimpleFunction && !overridesComposable()))

    private fun IrSimpleFunction.overridesComposable() =
        overriddenSymbols.any {
            it.owner.isDecoy() || it.owner.shouldBeRemapped()
        }

    private fun IrFunction.hasComposableParameter() =
        valueParameters.any { it.type.containsComposableAnnotation() } ||
            extensionReceiverParameter?.type.containsComposableAnnotation()

    private fun IrFunction.isEnumConstructor() =
        this is IrConstructor && parentAsClass.isEnumClass
}
