package hair.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.utils.*

// TODO Throw with handler -> Goto

class NormalizationImpl(val session: Session, builder: NodeBuilder) : Normalization {

    override fun normalize(node: Node): Node = node.accept(normalizer)

    val normalizer: NodeVisitor<Node> = context(builder) {
        object : NodeVisitor<Node>() {
            override fun visitNode(node: Node): Node = node

            // Arithmetic
            private fun tryFoldConstant(node: BinaryOp, op: (Int, Int) -> Int): ConstI? { // FIXME Int -> Number?
                val lhs = node.lhs
                val rhs = node.rhs
                if (lhs is ConstI && rhs is ConstI) {
                    return ConstI(op(lhs.value, rhs.value))
                }
                return null
            }

            override fun visitAdd(node: Add): Node {
                return tryFoldConstant(node) { l, r -> l + r }
                    ?: super.visitAdd(node)
            }

            // TODO sub etc
            // TODO maybe sub(x, y) is add(x, -y)?

            override fun visitBinaryOp(node: BinaryOp): Node {
                // TODO commutative, associative, etc : oder args uniformly
                // TODO Add(x, 0), etc
                return super.visitBinaryOp(node)
            }

            // TODO If(const) -> Goto
            override fun visitPhi(node: Phi): Node {
                if (node.joinedValues.toSet().size == 1) return node.joinedValues.first()
                return super.visitPhi(node)
            }

            override fun visitCatch(node: Catch): Node {
                // FIXME Phi(Catch) VS Catch(Phi)?
                // FIXME
//                val thrower = node.unwind.thrower
//                if (thrower is Throw) return thrower.exception

                return super.visitCatch(node)
            }

        }
    }
}
