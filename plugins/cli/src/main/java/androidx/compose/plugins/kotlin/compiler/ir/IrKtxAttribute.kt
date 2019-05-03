package androidx.compose.plugins.kotlin.compiler.ir

import org.jetbrains.kotlin.ir.IrKtxStatement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.psi.KtxAttribute
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class IrKtxAttribute(
    val element: KtxAttribute,
    val value: IrExpression
) : IrKtxStatement {
    override val startOffset = element.startOffset
    override val endOffset = element.endOffset
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitKtxStatement(this, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) =
        value.accept(visitor, data)
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        throw UnsupportedOperationException(
            "This temporary placeholder node must be manually transformed, not visited"
        )
    }
}
