package androidx.compose.plugins.kotlin.compiler.ir

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

fun IrElement.find(filter: (descriptor: IrElement) -> Boolean): Collection<IrElement> {
    val elements: MutableList<IrElement> = mutableListOf()
    accept(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (filter(element)) elements.add(element)
            element.acceptChildren(this, null)
        }
        override fun visitKtxStatement(expression: IrKtxStatement, data: Nothing?) {
            expression.acceptChildren(this, null)
        }
    }, null)
    return elements
}

inline fun <T : IrDeclaration> T.buildWithScope(
    context: GeneratorContext,
    builder: (T) -> Unit
): T =
    also { irDeclaration ->
        context.symbolTable.withScope(irDeclaration.descriptor) {
            builder(irDeclaration)
        }
    }
