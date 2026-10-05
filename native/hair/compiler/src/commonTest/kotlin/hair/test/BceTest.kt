/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package hair.test

import hair.compilation.Compilation
import hair.compilation.FunctionCompilation
import hair.graph.InequalityGraph
import hair.graph.buildInequalityGraph
import hair.inequality.Bound
import hair.inequality.Vertex
import hair.ir.*
import hair.ir.nodes.*
import hair.opt.eliminateBoundsCheck
import hair.opt.removePis
import hair.sym.HairType
import hair.transform.*

typealias IrBody = context(NodeBuilder, ArgumentUpdater, ControlFlowBuilder) () -> Unit

interface BceTest : IrTest {
    fun bceIR(vararg params: HairType, body: IrBody): FunctionCompilation {
        val compilation = Compilation(testConfig).getCompilation(Fun("f", params.toList()))
        with(compilation.session) {
            buildInitialIR { body() }
            insertPis()
            buildSSA()
        }
        return compilation
    }

    fun FunctionCompilation.graph(): InequalityGraph =
        context(this) { this.session.withValueTypes { this.session.buildInequalityGraph() } }

    fun FunctionCompilation.runBce() = context(this) {
        this.session.eliminateBoundsCheck()
        this.session.removePis()
    }

    val FunctionCompilation.checks: List<ArrayIndexCheck>
        get() = session.allNodes<ArrayIndexCheck>().toList()

    fun FunctionCompilation.piOf(origin: Node, value: Node): Pi =
        session.allNodes<Pi>().single { it.origin == origin && it.value == value }

    fun Vertex.hasPred(bound: Bound, from: Vertex, weight: Long): Boolean =
        preds(bound).any { it.from === from && it.weight == weight }
}

context(_: NodeBuilder, controlBuilder: ControlFlowBuilder, _: ArgumentUpdaterBase)
fun whileLoopWithHeader(
    cond: context(NodeBuilder, ControlFlowBuilder) () -> Node,
    body: context(NodeBuilder, ControlFlowBuilder) () -> Unit,
) {
    val header = BlockEntry(Goto(), null) as BlockEntry
    val [trueExit, falseExit] = IfExits(cond())
    BlockEntry(trueExit)
    body()
    header.preds[1] = Goto()
    BlockEntry(falseExit)
}
