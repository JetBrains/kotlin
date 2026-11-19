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

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrRichFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.selectSAMOverriddenFunction
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative

@Suppress("PRE_RELEASE_CLASS")
class ComposableFunInterfaceLowering(private val context: IrPluginContext) :
    IrElementTransformerVoidWithContext(),
    ModuleLoweringPass {

    override fun lower(irModule: IrModuleFragment) {
        if (context.platform.isJvm() || (context.platform.isNative() && context.languageVersionSettings.supportsFeature(LanguageFeature.IrRichCallableReferencesInKlibs))) {
            irModule.transformChildrenVoid(this)
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        val functionExpr = expression.findSamFunctionExpr()
        if (functionExpr != null && expression.typeOperand.isComposableFunInterface()) {
            val argument = functionExpr.transform(this, null) as IrFunctionExpression
            val superType = expression.typeOperand
            val superClass = superType.classOrNull ?: error("Expected non-null class")
            return if (!context.platform.isJvm()) {
                IrRichFunctionReferenceImpl(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = expression.typeOperand,
                    reflectionTargetSymbol = null,
                    overriddenFunctionSymbol = superClass.owner.selectSAMOverriddenFunction().symbol,
                    invokeFunction = functionExpr.function,
                    origin = functionExpr.origin
                )
            } else {
                FunctionReferenceBuilder(
                    argument,
                    superClass,
                    superType,
                    currentDeclarationParent!!,
                    context,
                    currentScope!!.scope.scopeOwnerSymbol,
                    IrTypeSystemContextImpl(context.irBuiltIns)
                ).build()
            }
        }
        return super.visitTypeOperator(expression)
    }
}
