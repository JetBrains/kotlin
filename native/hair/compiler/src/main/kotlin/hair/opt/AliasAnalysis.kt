package hair.opt

import hair.ir.initiallyEscaped
import hair.ir.nodes.AnyNew
import hair.ir.nodes.Escape
import hair.ir.nodes.Node
import hair.logic.Trilean
import hair.logic.TrileanLogic

interface AliasAnalysis {
    fun aliases(lhs: Node, rhs: Node): Trilean
}

class SimpleAliasAnalysis : AliasAnalysis {
    override fun aliases(lhs: Node, rhs: Node): Trilean = with(TrileanLogic) {
        reflexive(
            simmetrical { l: Node, r: Node ->
                when (l) {
                    is Escape ->
                        if (r is AnyNew && l.initiallyEscaped == r) Trilean.YES
                        else Trilean.MAYBE

                    is AnyNew ->
                        // FIXME copy&pasted
                        if (r is Escape && r.initiallyEscaped == l) Trilean.YES
                        else Trilean.NO

                    else -> Trilean.MAYBE
                }
            }
        )(lhs, rhs)
    }
}

