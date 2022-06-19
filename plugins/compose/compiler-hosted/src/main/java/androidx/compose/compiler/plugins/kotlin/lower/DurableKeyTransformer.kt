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
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrElseBranch
import org.jetbrains.kotlin.ir.expressions.IrEnumConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.IrVarargElement
import org.jetbrains.kotlin.ir.expressions.IrWhen
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrElseBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

open class DurableKeyTransformer(
    private val keyVisitor: DurableKeyVisitor,
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    metrics: ModuleMetrics,
) :
    AbstractComposeLowering(context, symbolRemapper, metrics),
    ModuleLoweringPass {

    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)
    }

    protected fun buildKey(
        prefix: String,
        pathSeparator: String = "/",
        siblingSeparator: String = ":"
    ): Pair<String, Boolean> = keyVisitor.buildPath(prefix, pathSeparator, siblingSeparator)

    protected fun <T> root(keys: MutableSet<String>, block: () -> T): T =
        keyVisitor.root(keys, block)
    protected fun <T> enter(key: String, block: () -> T) = keyVisitor.enter(key, block)
    protected fun <T> siblings(key: String, block: () -> T) = keyVisitor.siblings(key, block)
    protected fun <T> siblings(block: () -> T) = keyVisitor.siblings(block)

    protected fun Name.asJvmFriendlyString(): String {
        return if (!isSpecial) identifier
        else asString()
            .replace('<', '$')
            .replace('>', '$')
            .replace(' ', '-')
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        // constants in annotations need to be compile-time values, so we can never transform them
        if (declaration.isAnnotationClass) return declaration
        return siblings("class-${declaration.name.asJvmFriendlyString()}") {
            super.visitClass(declaration)
        }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        includeFileNameInExceptionTrace(declaration) {
            val filePath = declaration.fileEntry.name
            val fileName = filePath.split('/').last()
            return enter("file-$fileName") { super.visitFile(declaration) }
        }
    }

    override fun visitPackageFragment(declaration: IrPackageFragment): IrPackageFragment {
        return enter("pkg-${declaration.fqNameForIrSerialization}") {
            super.visitPackageFragment(declaration)
        }
    }

    override fun visitTry(aTry: IrTry): IrExpression {
        aTry.tryResult = enter("try") {
            aTry.tryResult.transform(this, null)
        }
        siblings {
            aTry.catches.forEach {
                it.result = enter("catch") { it.result.transform(this, null) }
            }
        }
        aTry.finallyExpression = enter("finally") {
            aTry.finallyExpression?.transform(this, null)
        }
        return aTry
    }

    override fun visitDelegatingConstructorCall(
        expression: IrDelegatingConstructorCall
    ): IrExpression {
        val owner = expression.symbol.owner

        // annotations are represented as constructor calls in IR, but the parameters need to be
        // compile-time values only, so we can't transform them at all.
        if (owner.parentAsClass.isAnnotationClass) return expression

        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        val owner = expression.symbol.owner
        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        val owner = expression.symbol.owner

        // annotations are represented as constructor calls in IR, but the parameters need to be
        // compile-time values only, so we can't transform them at all.
        if (owner.parentAsClass.isAnnotationClass) return expression

        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val owner = expression.symbol.owner
        val name = owner.name.asJvmFriendlyString()

        return enter("call-$name") {
            expression.dispatchReceiver = enter("\$this") {
                expression.dispatchReceiver?.transform(this, null)
            }
            expression.extensionReceiver = enter("\$\$this") {
                expression.extensionReceiver?.transform(this, null)
            }

            for (i in 0 until expression.valueArgumentsCount) {
                val arg = expression.getValueArgument(i)
                if (arg != null) {
                    enter("arg-$i") {
                        expression.putValueArgument(i, arg.transform(this, null))
                    }
                }
            }
            expression
        }
    }

    override fun visitEnumEntry(declaration: IrEnumEntry): IrStatement {
        return enter("entry-${declaration.name.asJvmFriendlyString()}") {
            super.visitEnumEntry(declaration)
        }
    }

    override fun visitVararg(expression: IrVararg): IrExpression {
        if (expression !is IrVarargImpl) return expression
        return enter("vararg") {
            expression.elements.forEachIndexed { i, arg ->
                expression.elements[i] = enter("$i") {
                    arg.transform(this, null) as IrVarargElement
                }
            }
            expression
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    protected fun IrType.asString(): String {
        return when (this) {
            is IrDynamicType -> "dynamic"
            is IrErrorType -> "IrErrorType"
            is IrSimpleType -> classifier.descriptor.name.asString()
            else -> "{${javaClass.simpleName} $this}"
        }
    }

    protected fun IrSimpleFunction.signatureString(): String {
        return buildString {
            extensionReceiverParameter?.let {
                append(it.type.asString())
                append(".")
            }
            append(name.asJvmFriendlyString())
            append('(')
            append(valueParameters.joinToString(",") { it.type.asString() })
            append(')')
            append(returnType.asString())
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val path = "fun-${declaration.signatureString()}"
        return enter(path) { super.visitSimpleFunction(declaration) }
    }

    override fun visitLoop(loop: IrLoop): IrExpression {
        return when (loop.origin) {
            // in these cases, the compiler relies on a certain structure for the condition
            // expression, so we only touch the body
            IrStatementOrigin.WHILE_LOOP,
            IrStatementOrigin.FOR_LOOP_INNER_WHILE -> enter("loop") {
                loop.body = enter("body") { loop.body?.transform(this, null) }
                loop
            }
            else -> enter("loop") {
                loop.condition = enter("cond") { loop.condition.transform(this, null) }
                loop.body = enter("body") { loop.body?.transform(this, null) }
                loop
            }
        }
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation): IrExpression {
        if (expression !is IrStringConcatenationImpl) return expression
        return enter("str") {
            siblings {
                expression.arguments.forEachIndexed { index, expr ->
                    expression.arguments[index] = enter("$index") {
                        expr.transform(this, null)
                    }
                }
                expression
            }
        }
    }

    override fun visitWhen(expression: IrWhen): IrExpression {
        return when (expression.origin) {
            // ANDAND needs to have an 'if true then false' body on its second branch, so only
            // transform the first branch
            IrStatementOrigin.ANDAND -> {
                expression.branches[0] = expression.branches[0].transform(this, null)
                expression
            }

            // OROR condition should have an 'if a then true' body on its first branch, so only
            // transform the second branch
            IrStatementOrigin.OROR -> {
                expression.branches[1] = expression.branches[1].transform(this, null)
                expression
            }

            IrStatementOrigin.IF -> siblings("if") {
                super.visitWhen(expression)
            }

            else -> siblings("when") {
                super.visitWhen(expression)
            }
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        return enter("param-${declaration.name.asJvmFriendlyString()}") {
            super.visitValueParameter(declaration)
        }
    }

    override fun visitElseBranch(branch: IrElseBranch): IrElseBranch {
        return IrElseBranchImpl(
            startOffset = branch.startOffset,
            endOffset = branch.endOffset,
            // the condition of an else branch is a constant boolean but we don't want
            // to convert it into a live literal, so we don't transform it
            condition = branch.condition,
            result = enter("else") {
                branch.result.transform(this, null)
            }
        )
    }

    override fun visitBranch(branch: IrBranch): IrBranch {
        return IrBranchImpl(
            startOffset = branch.startOffset,
            endOffset = branch.endOffset,
            condition = enter("cond") {
                branch.condition.transform(this, null)
            },
            // only translate the result, as the branch is a constant boolean but we don't want
            // to convert it into a live literal
            result = enter("branch") {
                branch.result.transform(this, null)
            }
        )
    }

    override fun visitComposite(expression: IrComposite): IrExpression {
        return siblings {
            super.visitComposite(expression)
        }
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        return when (expression.origin) {
            // The compiler relies on a certain structure for the "iterator" instantiation in For
            // loops, so we avoid transforming the first statement in this case
            IrStatementOrigin.FOR_LOOP,
            IrStatementOrigin.FOR_LOOP_INNER_WHILE -> {
                expression.statements[1] =
                    expression.statements[1].transform(this, null) as IrStatement
                expression
            }
//            IrStatementOrigin.SAFE_CALL
//            IrStatementOrigin.WHEN
//            IrStatementOrigin.IF
//            IrStatementOrigin.ELVIS
//            IrStatementOrigin.ARGUMENTS_REORDERING_FOR_CALL
            else -> siblings {
                super.visitBlock(expression)
            }
        }
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        val owner = expression.symbol.owner
        val name = owner.name
        return when (owner.origin) {
            // for these synthetic variable declarations we want to avoid transforming them since
            // the compiler will rely on their compile time value in some cases.
            IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE -> expression
            IrDeclarationOrigin.IR_TEMPORARY_VARIABLE -> expression
            IrDeclarationOrigin.FOR_LOOP_VARIABLE -> expression
            else -> enter("set-$name") { super.visitSetValue(expression) }
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        val name = expression.symbol.owner.name
        return enter("set-$name") { super.visitSetField(expression) }
    }

    override fun visitBlockBody(body: IrBlockBody): IrBody {
        return siblings {
            super.visitBlockBody(body)
        }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return enter("val-${declaration.name.asJvmFriendlyString()}") {
            super.visitVariable(declaration)
        }
    }

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val backingField = declaration.backingField
        val getter = declaration.getter
        val setter = declaration.setter
        val name = declaration.name.asJvmFriendlyString()

        return enter("val-$name") {
            // turn them into live literals. We should consider transforming some simple cases like
            // `val foo = 123`, but in general turning this initializer into a getter is not a
            // safe operation. We should figure out a way to do this for "static" expressions
            // though such as `val foo = 16.dp`.
            declaration.backingField = backingField?.transform(this, null) as? IrField
            declaration.getter = enter("get") {
                getter?.transform(this, null) as? IrSimpleFunction
            }
            declaration.setter = enter("set") {
                setter?.transform(this, null) as? IrSimpleFunction
            }
            declaration
        }
    }
}