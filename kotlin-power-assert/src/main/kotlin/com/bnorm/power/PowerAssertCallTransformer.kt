/*
 * Copyright (C) 2020 Brian Norman
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

package com.bnorm.power

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.asSimpleLambda
import org.jetbrains.kotlin.backend.common.ir.inline
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.io.File

fun FileLoweringPass.runOnFileInOrder(irFile: IrFile) {
  irFile.acceptVoid(object : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
      element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
      lower(declaration)
      super.visitFile(declaration)
    }
  })
}

class PowerAssertCallTransformer(
  private val context: JvmBackendContext
) : IrElementTransformerVoid(), FileLoweringPass {
  private lateinit var file: IrFile
  private lateinit var fileSource: String
  private val constructor: IrConstructorSymbol = context.ir.symbols.assertionErrorConstructor

  override fun lower(irFile: IrFile) {
    file = irFile
    fileSource = File(irFile.path).readText()

    irFile.transformChildrenVoid()
  }

  override fun visitCall(expression: IrCall): IrExpression {
    val function = expression.symbol.owner
    if (!function.isAssert)
      return super.visitCall(expression)

    val assertionArgument = expression.getValueArgument(0)!!
    val lambdaArgument = if (function.valueParameters.size == 2) expression.getValueArgument(1) else null

    context.createIrBuilder(expression.symbol).run {
      at(expression)

      val lambda = lambdaArgument?.asSimpleLambda()
      val title = when {
        lambda != null -> lambda.inline()
        lambdaArgument != null -> {
          val invoke = lambdaArgument.type.getClass()!!.functions.single { it.name == OperatorNameConventions.INVOKE }
          irCallOp(invoke.symbol, invoke.returnType, lambdaArgument)
        }
        else -> irString("Assertion failed")
      }

//      println(assertionArgument.dump())
//      println(tree.dump())

      val generator = object : PowerAssertGenerator() {
        override fun IrBuilderWithScope.buildAssertThrow(subStack: List<IrStackVariable>): IrExpression {
          return buildThrow(constructor, buildMessage(file, fileSource, title, expression, subStack))
        }
      }

      val tree = buildAssertTree(assertionArgument)
      val root = tree.children.single()
      return generator.buildAssert(this, root)
//        .also { println(it.dump())}
    }
  }
}

val IrFunction.isAssert: Boolean
  get() = name.asString() == "assert" && getPackageFragment()?.fqName == KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
