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

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isLambda
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.platform.jvm.isJvm

@Suppress("PRE_RELEASE_CLASS")
class ComposableFunInterfaceLowering(private val context: IrPluginContext) :
    IrElementTransformerVoidWithContext(),
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        if (context.platform.isJvm()) {
            module.transformChildrenVoid(this)
        }
    }

    private fun isFunInterfaceConversion(expression: IrTypeOperatorCall): Boolean {
        val argument = expression.argument
        val operator = expression.operator
        val type = expression.typeOperand
        val functionClass = type.classOrNull
        return operator == IrTypeOperator.SAM_CONVERSION &&
            argument is IrFunctionExpression &&
            argument.origin.isLambda &&
            functionClass != null &&
            functionClass.owner.isFun
        // IMPORTANT(b/178663739):
        // We are transforming not just SAM conversions for composable fun interfaces, but ALL
        // fun interfaces temporarily until KT-44622 gets fixed in the version of kotlin we
        // are using, which should be in 1.4.30.
        // Once it does, we should either add the below additional condition to this predicate,
        // or, if possible, remove this lowering all together if kotlin's lowering works for
        // composable fun interfaces as well.
        //
        // functionClass.functions.single {
        //    it.owner.modality == Modality.ABSTRACT
        // }.owner.annotations.hasAnnotation(ComposeFqNames.Composable)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (isFunInterfaceConversion(expression)) {
            val argument = expression.argument.transform(this, null) as IrFunctionExpression
            val superType = expression.typeOperand
            val superClass = superType.classOrNull ?: error("Expected non-null class")
            return FunctionReferenceBuilder(
                argument,
                superClass,
                superType,
                currentDeclarationParent!!,
                context,
                currentScope!!.scope.scopeOwnerSymbol,
                IrTypeSystemContextImpl(context.irBuiltIns)
            ).build()
        }
        return super.visitTypeOperator(expression)
    }
}
