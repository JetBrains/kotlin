package hair.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.sym.ArithmeticType
import hair.utils.ensuring

// TODO Throw with handler -> Goto

class Normalization(val session: Session, nodeBuilder: NodeBuilder, argsUpdater: ArgumentUpdater) {

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
                // -const[x] => const[-x]
                is ConstAny -> Const(
                    when (val value = operand.numberValue) {
                        is Int -> -value
                        is Long -> -value
                        is Float -> -value
                        is Double -> -value
                        else -> error("Should not reach here $value")
                    }
                )
                // - -a => a
                is Neg -> operand.operand

                else -> node
            }

            private fun normalizeBinary(op: BinaryOpKind, node: BinaryOp, lhs: Node, rhs: Node, builder: (Node, Node) -> Node): Node {
                if (rhs is ConstAny) {
                    // const[a] op const[b] => const[a op b]
                    if (lhs is ConstAny)
                        return Const(op.op(lhs.numberValue, rhs.numberValue))

                    // a op identity => a
                    if (rhs.numberValue == op.identity)
                        return lhs
                }

                if (op.associative && lhs is BinaryOp && lhs.form == node.form) {
                    // (a op const[b]) op const[c] => a op const[b op c]
                    if (lhs.rhs is ConstAny && rhs is ConstAny)
                        return builder(lhs.lhs, Const(op.op((lhs.rhs as ConstAny).numberValue, rhs.numberValue)))

                    // (a op b) op (c op d) => ((a op b) op c) op d
                    if (rhs is BinaryOp && rhs.form == node.form)
                        return builder(builder(builder(lhs.lhs, lhs.rhs), rhs.lhs), rhs.rhs)
                }

                // When callers "swap" their operands, they just reorder parameters to this function (lhs == node.rhs).
                // In that case, we should rebuild this node.
                if (lhs != node.lhs || rhs != node.rhs)
                    return builder(lhs, rhs)

                return node
            }

            private fun canonicalArgs(lhs: Node, rhs: Node): Pair<Node, Node> =
                if (needSwap(lhs, rhs)) rhs to lhs else lhs to rhs

            private fun needSwap(lhs: Node, rhs: Node): Boolean = when {
                rhs is ConstAny -> false
                lhs is ConstAny -> true
                rhs is BinaryOp && lhs !is BinaryOp -> true
                lhs is BinaryOp -> false
                rhs.id > lhs.id -> true
                else -> false
            }

            override fun visitAdd(node: Add): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.ADD_INT
                    ArithmeticType.LONG -> BinaryOpKind.ADD_LONG
                    ArithmeticType.FLOAT -> BinaryOpKind.ADD_FLOAT
                    ArithmeticType.DOUBLE -> BinaryOpKind.ADD_DOUBLE
                }

                val [lhs, rhs] = canonicalArgs(node.lhs, node.rhs)

                // -a + b => b - a
                if (lhs is Neg) {
                    return Sub(node.opType)(rhs, lhs.operand)
                }

                // a + -b => a - b
                if (rhs is Neg) {
                    return Sub(node.opType)(lhs, rhs.operand)
                }

                // a + a => a << const[1]
                if (node.opType.isIntegral && lhs == rhs) {
                    return Shl(node.opType)(lhs, ConstI(1))
                }

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Add(node.opType)(lhs, rhs) }
            }

            override fun visitSub(node: Sub): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.SUB_INT
                    ArithmeticType.LONG -> BinaryOpKind.SUB_LONG
                    ArithmeticType.FLOAT -> BinaryOpKind.SUB_FLOAT
                    ArithmeticType.DOUBLE -> BinaryOpKind.SUB_DOUBLE
                }

                val [lhs, rhs] = node.lhs to node.rhs

                // a - a => const[0]
                if (node.opType.isIntegral && rhs == lhs)
                    return Const(node.opType, 0)

                // a - -b => a + b
                if (rhs is Neg)
                    return Add(node.opType)(lhs, rhs.operand)

                // -a - b => -(a + b)
                if (node.opType.isIntegral && lhs is Neg)
                    return Neg(Add(node.opType)(lhs, rhs))

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Sub(node.opType)(lhs, rhs) }
            }

            override fun visitMul(node: Mul): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.MUL_INT
                    ArithmeticType.LONG -> BinaryOpKind.MUL_LONG
                    ArithmeticType.FLOAT -> BinaryOpKind.MUL_FLOAT
                    ArithmeticType.DOUBLE -> BinaryOpKind.MUL_DOUBLE
                }

                val [lhs, rhs] = canonicalArgs(node.lhs, node.rhs)

                // TODO: Support for NaN and infinities
                // a * const[0] => const[0]
                if (node.opType.isIntegral && rhs is ConstAny && rhs.numberValue == 0)
                    return Const(node.opType, 0)

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Mul(node.opType)(lhs, rhs) }
            }

            override fun visitDiv(node: Div): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.DIV_INT
                    ArithmeticType.LONG -> BinaryOpKind.DIV_LONG
                    ArithmeticType.FLOAT -> BinaryOpKind.DIV_FLOAT
                    ArithmeticType.DOUBLE -> BinaryOpKind.DIV_DOUBLE
                }

                val [lhs, rhs] = node.lhs to node.rhs

                // TODO: a / const[0] => throwable

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Div(node.opType)(lhs, rhs) }
            }

            override fun visitRem(node: Rem): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.REM_INT
                    ArithmeticType.LONG -> BinaryOpKind.REM_LONG
                    ArithmeticType.FLOAT -> BinaryOpKind.REM_FLOAT
                    ArithmeticType.DOUBLE -> BinaryOpKind.REM_DOUBLE
                }

                val [lhs, rhs] = node.lhs to node.rhs

                // TODO: a / const[0] => throwing - float and double return NaN?

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Rem(node.opType)(lhs, rhs) }
            }

            override fun visitInv(node: Inv): Node = when (val operand = node.operand) {
                is ConstAny -> Const(
                    when (val value = operand.numberValue) {
                        is Int -> value.inv()
                        is Long -> value.inv()
                        else -> error("Should not reach here $node")
                    }
                )

                // ~ ~a => a
                is Inv -> operand.operand

                else -> node
            }

            override fun visitAnd(node: And): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.AND_INT
                    ArithmeticType.LONG -> BinaryOpKind.AND_LONG
                    else -> error("Should not reach here $node")
                }

                val [lhs, rhs] = canonicalArgs(node.lhs, node.rhs)

                // a & const[0] => const[0]
                if (rhs is ConstAny && rhs.numberValue == 0)
                    return Const(node.opType, 0)

                // a & a => a
                if (lhs == rhs)
                    return lhs

                if (rhs is Inv) {
                    // a & ~a => const[0]
                    if (rhs.operand == lhs)
                        return Const(node.opType, 0)

                    // ~a & ~b => ~(a | b)
                    if (lhs is Inv)
                        return Inv(Or(node.opType)(lhs.operand, rhs.operand))
                }

                // (a | b) & a => a
                if (lhs is Or && (lhs.lhs == rhs || lhs.rhs == rhs))
                    return rhs

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> And(node.opType)(lhs, rhs) }
            }

            override fun visitOr(node: Or): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.OR_INT
                    ArithmeticType.LONG -> BinaryOpKind.OR_LONG
                    else -> error("Should not reach here $node")
                }

                val [lhs, rhs] = canonicalArgs(node.lhs, node.rhs)

                // a | const[-1] => const[-1]
                if (rhs is ConstAny && rhs.numberValue == -1)
                    return Const(node.opType, -1)

                // a | a => a
                if (lhs == rhs)
                    return lhs

                if (rhs is Inv) {
                    // a | ~a => -1
                    if (rhs.operand == lhs)
                        return Const(node.opType, -1)

                    // ~a | ~b => ~(a | b)
                    if (lhs is Inv)
                        return Inv(And(node.opType)(lhs.operand, rhs.operand))
                }

                // (a & b) | a => a
                if (lhs is And && (lhs.lhs == rhs || lhs.rhs == rhs))
                    return rhs

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Or(node.opType)(lhs, rhs) }
            }

            override fun visitXor(node: Xor): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.XOR_INT
                    ArithmeticType.LONG -> BinaryOpKind.XOR_LONG
                    else -> error("Should not reach here $node")
                }

                val [lhs, rhs] = canonicalArgs(node.lhs, node.rhs)

                // a ^ const[-1] => ~a
                if (rhs is ConstAny && rhs.numberValue == -1)
                    return Inv(lhs)

                // a ^ a => const[0]
                if (lhs == rhs)
                    return Const(node.opType, 0)

                // (a ^ b) ^ a => b
                if (lhs is Xor) {
                    if (lhs.lhs == rhs)
                        return lhs.rhs

                    if (lhs.rhs == rhs)
                        return lhs.lhs
                }

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Xor(node.opType)(lhs, rhs) }
            }

            override fun visitShl(node: Shl): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.SHL_INT
                    ArithmeticType.LONG -> BinaryOpKind.SHL_LONG
                    else -> error("Should not reach here $node")
                }

                val [lhs, rhs] = node.lhs to node.rhs

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Shl(node.opType)(lhs, rhs) }
            }

            override fun visitShr(node: Shr): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.SHR_INT
                    ArithmeticType.LONG -> BinaryOpKind.SHR_LONG
                    else -> error("Should not reach here $node")
                }

                return normalizeBinary(opKind, node, node.lhs, node.rhs) { lhs, rhs -> Shr(node.opType)(lhs, rhs) }
            }

            override fun visitUshr(node: Ushr): Node {
                val opKind = when (node.opType) {
                    ArithmeticType.INT -> BinaryOpKind.USHR_INT
                    ArithmeticType.LONG -> BinaryOpKind.USHR_LONG
                    else -> error("Should not reach here $node")
                }

                val [lhs, rhs] = node.lhs to node.rhs

                return normalizeBinary(opKind, node, lhs, rhs) { lhs, rhs -> Ushr(node.opType)(lhs, rhs) }
            }

            override fun visitNot(node: Not): Node = when (val operand = node.operand) {
                // !!a => a
                is Not -> operand.operand

                else -> node
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

enum class BinaryOpKind(
    val associative: Boolean,
    val identity: Number?,
    val op: (Number, Number) -> Number,
) {
    // FIXME toInt
    ADD_INT(true, 0, { l, r -> l.toInt() + r.toInt() }),
    ADD_LONG(true, 0L, { l, r -> l.toLong() + r.toLong() }),
    ADD_FLOAT(false, 0.0f, { l, r -> l.toFloat() + r.toFloat() }),
    ADD_DOUBLE(false, 0.0, { l, r -> l.toDouble() + r.toDouble() }),

    SUB_INT(false, 0, { l, r -> l.toInt() - r.toInt() }),
    SUB_LONG(false, 0L, { l, r -> l.toLong() - r.toLong() }),
    SUB_FLOAT(false, 0.0f, { l, r -> l.toFloat() - r.toFloat() }),
    SUB_DOUBLE(false, 0.0, { l, r -> l.toDouble() - r.toDouble() }),

    MUL_INT(true, 1, { l, r -> l.toInt() * r.toInt() }),
    MUL_LONG(true, 1L, { l, r -> l.toLong() * r.toLong() }),
    MUL_FLOAT(false, 1.0f, { l, r -> l.toFloat() * r.toFloat() }),
    MUL_DOUBLE(false, 1.0, { l, r -> l.toDouble() * r.toDouble() }),

    DIV_INT(false, 1, { l, r -> l.toInt() / r.toInt() }),
    DIV_LONG(false, 1L, { l, r -> l.toLong() / r.toLong() }),
    DIV_FLOAT(false, 1.0f, { l, r -> l.toFloat() / r.toFloat() }),
    DIV_DOUBLE(false, 1.0, { l, r -> l.toDouble() / r.toDouble() }),

    REM_INT(false, null, { l, r -> l.toInt() % r.toInt() }),
    REM_LONG(false, null, { l, r -> l.toLong() % r.toLong() }),
    REM_FLOAT(false, null, { l, r -> l.toFloat() % r.toFloat() }),
    REM_DOUBLE(false, null, { l, r -> l.toDouble() % r.toDouble() }),

    AND_INT(true, -1, { l, r -> l.toInt() and r.toInt() }),
    AND_LONG(true, -1L, { l, r -> l.toLong() and r.toLong() }),

    OR_INT(true, 0, { l, r -> l.toInt() or r.toInt() }),
    OR_LONG(true, 0L, { l, r -> l.toLong() or r.toLong() }),

    XOR_INT(true, 0, { l, r -> l.toInt() xor r.toInt() }),
    XOR_LONG(true, 0L, { l, r -> l.toLong() xor r.toLong() }),

    SHL_INT(false, 0, { l, r -> l.toInt() shl r.toInt() }),
    SHL_LONG(false, 0L, { l, r -> l.toLong() shl r.toInt() }),

    SHR_INT(false, 0, { l, r -> l.toInt() shr r.toInt() }),
    SHR_LONG(false, 0L, { l, r -> l.toLong() shr r.toInt() }),

    USHR_INT(false, 0, { l, r -> l.toInt() ushr r.toInt() }),
    USHR_LONG(false, 0L, { l, r -> l.toLong() ushr r.toInt() }),
}
