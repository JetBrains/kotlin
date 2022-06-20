/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * This transformer is a workaround for https://youtrack.jetbrains.com/issue/KT-44945 on non-JVM
 * targets. Once KT-44945 is fixed (we expect it in 1.5.0), this Transformer can be removed
 *
 * Converts assignable parameters to the corresponding variables and adds them on top of the
 * function
 *
 * E.g.
 * fun A($composer: Composer) {
 *     $composer = $composer.startRestartGroup(...)
 * }
 *
 * is converted to:
 *
 * fun A($composer: Composer) {
 *     var $composer = $composer
 *     $composer = $composer.startRestartGroup(...)
 * }
 */
class KlibAssignableParamTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    metrics: ModuleMetrics,
) : AbstractComposeLowering(context, symbolRemapper, metrics), ModuleLoweringPass {
    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val assignableParams = declaration.valueParameters.filter { it.isAssignable }

        if (assignableParams.isEmpty()) {
            return super.visitFunction(declaration)
        }

        val variables = assignableParams.map {
            val variable = IrVariableImpl(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.DEFINED,
                symbol = IrVariableSymbolImpl(),
                name = it.name,
                type = it.type,
                isVar = true,
                isConst = false,
                isLateinit = false
            )
            variable.parent = declaration

            variable.initializer = IrGetValueImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                it.symbol
            )

            variable
        }

        declaration.body = declaration.body?.let { body ->
            IrBlockBodyImpl(
                body.startOffset,
                body.endOffset
            ) {
                statements.addAll(variables)

                val updatedBody = body.statements.map {
                    it.transformStatement(
                        object : IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                if (expression.symbol.owner in assignableParams) {
                                    val paramIndex =
                                        assignableParams.indexOf(expression.symbol.owner)
                                    return super.visitGetValue(
                                        IrGetValueImpl(
                                            expression.startOffset,
                                            expression.endOffset,
                                            expression.type,
                                            variables[paramIndex].symbol,
                                            expression.origin
                                        )
                                    )
                                }
                                return super.visitGetValue(expression)
                            }

                            override fun visitSetValue(expression: IrSetValue): IrExpression {
                                if (expression.symbol.owner in assignableParams) {
                                    val paramIndex =
                                        assignableParams.indexOf(expression.symbol.owner)
                                    return super.visitSetValue(
                                        IrSetValueImpl(
                                            expression.startOffset,
                                            expression.endOffset,
                                            expression.type,
                                            variables[paramIndex].symbol,
                                            expression.value,
                                            expression.origin
                                        )
                                    )
                                }
                                return super.visitSetValue(expression)
                            }
                        }
                    )
                }

                statements.addAll(updatedBody)
            }
        }

        return super.visitFunction(declaration)
    }
}