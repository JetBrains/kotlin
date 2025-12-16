package hair.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.sym.HairType
import hair.utils.ensuring

// TODO Throw with handler -> Goto

class Normalization(val session: Session, nodeBuilder: NodeBuilder, argsUpdater: ArgsUpdater) {

    fun normalize(node: Node): Node {
        if (node.args.any { it == null }) return node
        return node.accept(normalizer).ensuring { it.accept(normalizer) == it }
    }

    private val normalizer: NodeVisitor<Node> = context(nodeBuilder, argsUpdater, NoControlFlowBuilder) {
        object : NodeVisitor<Node>() {
            override fun visitNode(node: Node): Node = node

            override fun visitControlled(node: Controlled): Node {
                if (node.control is Unreachable) return node.control
                return super.visitControlled(node)
            }

            override fun visitIfProjection(node: IfProjection): Node {
                // FIXME cant use owner here cause it tries to cast to If
                if (node.args[0] is Unreachable) return session.unreachable
                return super.visitIfProjection(node)
            }

            // Arithmetic

            override fun visitNeg(node: Neg): Node = when (val operand = node.operand) {
                is ConstAny -> Const(
                    when (val value = operand.numberValue) {
                        is Int -> -value
                        is Long -> -value
                        is Float -> -value
                        is Double -> -value
                        else -> error("Should not reach here $value")
                    }
                )
                is Neg -> operand.operand
                else -> node
            }

            private fun normalizeBinary(op: BinaryOpKind, node: BinaryOp, lhs: Node, rhs: Node, builder: (Node, Node) -> Node): Node {
                // fold constant
                if (lhs is ConstAny && rhs is ConstAny) {
                    return Const(op.op(lhs.numberValue, rhs.numberValue))
                }

                // fold with identity
                if (rhs is ConstAny && rhs == op.identity) return lhs

                // TODO use associativity and commutativity for complex trees

                // try to reorder operands
                if (op.commutative) {
                    val (newLhs, newRhs) = if (lhs is ConstAny) {
                        rhs to lhs
                    } else if (rhs is Add && lhs !is Add) {
                        rhs to lhs
                    } else {
                        // FIXME de we want to order arbitrary operands? e.g. to GVN Add(x, y) and Add(y, x)? How?
                        return node
                    }
                    // TODO would be nice to do just: node.form(rhs, lhs)
                    return builder(newLhs, newRhs)
                }

                return node
            }

            override fun visitAdd(node: Add): Node {
                if (node.lhs is Neg) {
                    return Sub(node.type)(node.rhs, (node.lhs as Neg).operand)
                }
                if (node.rhs is Neg) {
                    return Sub(node.type)(node.lhs, (node.rhs as Neg).operand)
                }
                val opKind = when (node.type) {
                    HairType.INT -> BinaryOpKind.ADD_INT
                    HairType.LONG -> BinaryOpKind.ADD_LONG
                    HairType.FLOAT -> BinaryOpKind.ADD_FLOAT
                    HairType.DOUBLE -> BinaryOpKind.ADD_DOUBLE
                    else -> error("Should not reach here $node")
                }
                return normalizeBinary(opKind, node, node.lhs, node.rhs) { lhs, rhs -> Add(node.type)(lhs, rhs) }
            }

            override fun visitSub(node: Sub): Node {
                if (node.rhs == node.lhs) return Const(node.type, 0)
                // TODO what is lhs is Neg?
                if (node.rhs is Neg) {
                    return Add(node.type)(node.lhs, (node.rhs as Neg).operand)
                }
                val opKind = when (node.type) {
                    HairType.INT -> BinaryOpKind.SUB_INT
                    HairType.LONG -> BinaryOpKind.SUB_LONG
                    HairType.FLOAT -> BinaryOpKind.SUB_FLOAT
                    HairType.DOUBLE -> BinaryOpKind.SUB_DOUBLE
                    else -> error("Should not reach here $node")
                }
                return normalizeBinary(opKind, node, node.lhs, node.rhs) { lhs, rhs -> Sub(node.type)(lhs, rhs) }
            }

            // TODO the rest


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

val ConstAny.numberValue: Number
    get() = when (this) {
        is ConstI -> value
        is ConstL -> value
        is ConstF -> value
        is ConstD -> value
        else -> error("Should not reach here $this")
    }

enum class BinaryOpKind(val associative: Boolean, val commutative: Boolean, val identity: Number, val op: (Number, Number) -> Number) {
    // FIXME toInt
    ADD_INT(true, true, 0, { l, r -> l.toInt() + r.toInt() }),
    ADD_LONG(true, true, 0L, { l, r -> l.toLong() + r.toLong() }),
    ADD_FLOAT(false, true, 0.0f, { l, r -> l.toFloat() + r.toFloat() }),
    ADD_DOUBLE(false, true, 0.0, { l, r -> l.toDouble() + r.toDouble() }),

    SUB_INT(false, false, 0, { l, r -> l.toInt() - r.toInt() }),
    SUB_LONG(false, false, 0L, { l, r -> l.toLong() - r.toLong() }),
    SUB_FLOAT(false, false, 0.0f, { l, r -> l.toFloat() - r.toFloat() }),
    SUB_DOUBLE(false, false, 0.0, { l, r -> l.toDouble() - r.toDouble() }),

    MUL_INT(true, true, 1, { l, r -> l.toInt() * r.toInt() }),
    MUL_LONG(true, true, 1L, { l, r -> l.toLong() * r.toLong() }),
    MUL_FLOAT(false, true, 1.0f, { l, r -> l.toFloat() * r.toFloat() }),
    MUL_DOUBLE(false, true, 1.0, { l, r -> l.toDouble() * r.toDouble() }),

    // TODO the rest
}