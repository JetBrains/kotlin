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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.lower.decoys.DecoyFqNames
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ComposerIntrinsicTransformer(
    val context: IrPluginContext,
    private val decoysEnabled: Boolean
) :
    IrElementTransformerVoid(),
    FileLoweringPass,
    ModuleLoweringPass {

    private val currentComposerIntrinsic = currentComposerFqName()

    // get-currentComposer gets transformed as decoy, as the getter now has additional params
    private fun currentComposerFqName(): FqName =
        if (decoysEnabled) {
            DecoyFqNames.CurrentComposerIntrinsic
        } else {
            ComposeFqNames.CurrentComposerIntrinsic
        }

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitCall(expression: IrCall): IrExpression {
        val calleeFqName = expression.symbol.descriptor.fqNameSafe
        if (calleeFqName == currentComposerIntrinsic) {
            // since this call was transformed by the ComposerParamTransformer, the first argument
            // to this call is the composer itself. We just replace this expression with the
            // argument expression and we are good.
            val expectedArgumentsCount = 1 + // composer parameter
                1 // changed parameter
            assert(expression.valueArgumentsCount == expectedArgumentsCount) {
                """
                    Composer call doesn't match expected argument count:
                        expected: $expectedArgumentsCount,
                        actual: ${expression.valueArgumentsCount},
                        expression: ${expression.dump()}
                """.trimIndent()
            }
            val composerExpr = expression.getValueArgument(0)
            if (composerExpr == null) error("Expected non-null composer argument")
            return composerExpr
        }
        return super.visitCall(expression)
    }
}
