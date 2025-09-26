package hair.opt

import hair.ir.*
import hair.ir.nodes.*
import hair.utils.*

class NormalizationImpl(val session: Session) : Normalization {
    private var builder: NodeBuilder? = null

    fun installBuilder(builder: NodeBuilder) {
        require(this.builder == null)
        this.builder = builder
    }

    override fun normalize(node: Node): Node = node.accept(normalizer)

    // FIXME allocation on each normalization is a huge overhead
    // TODO don't forget that builder is allocated on each request as well
    val normalizer: NodeVisitor<Node> by lazy {
        object : NodeVisitor<Node>(), NodeBuilder by builder!! {
            override fun visitNode(node: Node): Node = node

            // Arithmetic
            private fun tryFoldConstant(node: BinaryOp, op: (Long, Long) -> Long): ConstInt? { // FIXME Int -> Number?
                val lhs = node.lhs
                val rhs = node.rhs
                if (lhs is ConstInt && rhs is ConstInt) {
                    return ConstInt(op(lhs.value, rhs.value))
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

            override fun visitIndistinctMemory(node: IndistinctMemory): Node {
                fun IndistinctMemory.trySplitByPhi(): Node? {
                    val groupedByPhiBlock = inputs.groupBy {
                        (it as? Phi)?.block
                    }
                    // TODO this probably needs testing
                    val replacementInputs = groupedByPhiBlock.flatMap { (phiBlock, inputs) ->
                        if (phiBlock != null && inputs.size > 1) {
                            val joinedValuesForEachPhi = inputs.map { (it as Phi).joinedValues.toList() }
                            val inputsForEachPredBlock = joinedValuesForEachPhi.transpose()
                            val newPhiInputs = inputsForEachPredBlock.map { IndistinctMemory(*it.toTypedArray()) }
                            listOf(Phi(phiBlock, *newPhiInputs.toTypedArray()))
                        } else inputs
                    }
                    return if (replacementInputs.toSet() != inputs.toSet()) {
                        IndistinctMemory(*replacementInputs.toTypedArray())
                    } else null
                }
                node.trySplitByPhi()?.let { return it } // FIXME what for syntax a construction

                val replacementInputs = node.inputs.closure {
                    when (it) {
                        is IndistinctMemory -> node.inputs
                        else -> emptyList()
                    }
                }.filter { it !is IndistinctMemory }.toSet()
                if (replacementInputs.size == 1) return replacementInputs.single()

                if (replacementInputs.size != node.inputs.size || replacementInputs != node.inputs.toSet()) {
                    return IndistinctMemory(*replacementInputs.toTypedArray())
                }
                return node
            }

            // TODO If(const) -> Goto
            override fun visitPhi(node: Phi): Node {
                if (node.joinedValues.toSet().size == 1) return node.joinedValues.first()
                return super.visitPhi(node)
            }

        }
    }
}
