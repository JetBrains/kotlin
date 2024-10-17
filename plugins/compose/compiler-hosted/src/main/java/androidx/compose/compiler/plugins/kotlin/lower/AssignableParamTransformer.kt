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

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.StabilityInferencer
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
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// TODO fix description
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
class AssignableParamTransformer(
    val isJvm: Boolean,
    context: IrPluginContext,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    featureFlags: FeatureFlags,
) : AbstractComposeLowering(
    context,
    metrics,
    stabilityInferencer,
    featureFlags
), ModuleLoweringPass {
    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        val assignableParams = declaration.valueParameters.filter { it.isAssignable }

        if (assignableParams.isEmpty()) {
            return super.visitFunction(declaration)
        }

        val param2var = assignableParams.associateWith {
            if (isJvm) {
                it
            } else {
                IrVariableImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = IrDeclarationOrigin.DEFINED,
                    symbol = IrVariableSymbolImpl(),
                    name = it.name,
                    type = it.type,
                    isVar = true,
                    isConst = false,
                    isLateinit = false
                ).also { variable ->
                    variable.parent = declaration

                    variable.initializer = IrGetValueImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        it.symbol
                    )
                }
            }
        }

        declaration.body = declaration.body?.let { body ->
            context.irFactory.createBlockBody(
                body.startOffset,
                body.endOffset
            ).apply {
                if (!isJvm) {
                    statements.addAll(param2var.values)
                }

                val updatedBody = body.statements.map {
                    it.transformStatement(
                        object : IrElementTransformerVoid() {
                            override fun visitGetValue(expression: IrGetValue): IrExpression {
                                param2var[expression.symbol.owner]?.let { valueDeclaration ->
                                    return IrTypeOperatorCallImpl(
                                        expression.startOffset,
                                        expression.endOffset,
                                        expression.type,
                                        IrTypeOperator.IMPLICIT_CAST,
                                        expression.type,
                                        IrGetValueImpl(
                                            expression.startOffset,
                                            expression.endOffset,
                                            expression.type.defaultParameterType(),
                                            valueDeclaration.symbol,
                                            expression.origin
                                        )
                                    )
                                }
                                return super.visitGetValue(expression)
                            }

                            override fun visitSetValue(expression: IrSetValue): IrExpression {
                                val valueDeclaration = param2var[expression.symbol.owner]
                                if (!isJvm && valueDeclaration != null) {
                                    return super.visitSetValue(
                                        IrSetValueImpl(
                                            expression.startOffset,
                                            expression.endOffset,
                                            expression.type, // TODO defaultParameterType?
                                            valueDeclaration.symbol,
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
