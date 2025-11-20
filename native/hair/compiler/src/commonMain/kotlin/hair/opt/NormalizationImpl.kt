package hair.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.utils.*

// TODO Throw with handler -> Goto

class NormalizationImpl(val session: Session, nodeBuilder: NodeBuilder, argsUpdater: ArgsUpdater) : Normalization {

    override fun normalize(node: Node): Node {
        if (node.args.any { it == null }) return node
        return node.accept(normalizer)
    }

    val normalizer: NodeVisitor<Node> = context(nodeBuilder, argsUpdater) {
        object : NodeVisitor<Node>() {
            override fun visitNode(node: Node): Node = node

            override fun visitBlockEntry(node: BlockEntry): Node {
                if (node == session.entry) return node
                if (node.preds.isEmpty) return session.unreachable
                val singlePred = node.preds.singleOrNull()
                if (singlePred is Unreachable) return singlePred
                if (singlePred is Goto) {
                    val replacement = singlePred.control
                    singlePred.control = session.unreachable
                    return replacement
                }
                return super.visitBlockEntry(node)
            }

            override fun visitControlled(node: Controlled): Node {
                if (node.control is Unreachable) return node.control
                return super.visitControlled(node)
            }

            override fun visitIfProjection(node: IfProjection): Node {
                // FIXME cant use owner here
                if (node.args[0] is Unreachable) return session.unreachable
                return super.visitIfProjection(node)
            }

            override fun visitGoto(node: Goto): Node {
                (node.control as? BlockEntry)?.let {
                    val singlePred = it.preds.singleOrNull()
                    if (singlePred != null) {
                        it.preds[0] = session.unreachable
                        return singlePred
                    }
                }
                return super.visitGoto(node)
            }

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
