package hair.ir
import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.*

class ShallowNodeCloner(val nodeBuilder: NodeBuilder): NodeVisitor<Node>() {
    override fun visitNode(node: Node): Node = error("Should not reach here $node")

    override fun visitNoValue(node: NoValue): NoValue = context(nodeBuilder, NoControlFlowBuilder) { NoValue() }

    override fun visitUse(node: Use): Use = context(nodeBuilder, NoControlFlowBuilder) { Use(null, null) }

    override fun visitBlockEntry(node: BlockEntry): BlockEntry = context(nodeBuilder, NoControlFlowBuilder) { BlockEntry(*Array(node.preds.size) { null }) }

    override fun visitReturn(node: Return): Return = context(nodeBuilder, NoControlFlowBuilder) { Return(null, null) }

    override fun visitGoto(node: Goto): Goto = context(nodeBuilder, NoControlFlowBuilder) { Goto(null) }

    override fun visitIfTrue(node: If.True): If.True = context(nodeBuilder, NoControlFlowBuilder) { IfTrue(null) }

    override fun visitIfFalse(node: If.False): If.False = context(nodeBuilder, NoControlFlowBuilder) { IfFalse(null) }

    override fun visitIf(node: If): If = context(nodeBuilder, NoControlFlowBuilder) { If(null, null) }

    override fun visitThrow(node: Throw): Throw = context(nodeBuilder, NoControlFlowBuilder) { Throw(null, null) }

    override fun visitUnwind(node: Unwind): Unwind = context(nodeBuilder, NoControlFlowBuilder) { Unwind(null) }

    override fun visitReadVar(node: ReadVar): ReadVar = context(nodeBuilder, NoControlFlowBuilder) { ReadVar(node.variable)(null) }

    override fun visitAssignVar(node: AssignVar): AssignVar = context(nodeBuilder, NoControlFlowBuilder) { AssignVar(node.variable)(null, null) }

    override fun visitPhi(node: Phi): Phi = context(nodeBuilder, NoControlFlowBuilder) { Phi(null, *Array(node.joinedValues.size) { null }) } as Phi

    override fun visitParam(node: Param): Param = context(nodeBuilder, NoControlFlowBuilder) { Param(node.index) }

    override fun visitCatch(node: Catch): Catch = context(nodeBuilder, NoControlFlowBuilder) { Catch(null) } as Catch

    override fun visitConstI(node: ConstI): ConstI = context(nodeBuilder, NoControlFlowBuilder) { ConstI(node.value) }

    override fun visitAddI(node: AddI): AddI = context(nodeBuilder, NoControlFlowBuilder) { AddI(null, null) } as AddI

    override fun visitSubI(node: SubI): SubI = context(nodeBuilder, NoControlFlowBuilder) { SubI(null, null) } as SubI

    override fun visitMulI(node: MulI): MulI = context(nodeBuilder, NoControlFlowBuilder) { MulI(null, null) } as MulI

    override fun visitDivI(node: DivI): DivI = context(nodeBuilder, NoControlFlowBuilder) { DivI(null, null) } as DivI

    override fun visitRemI(node: RemI): RemI = context(nodeBuilder, NoControlFlowBuilder) { RemI(null, null) } as RemI

    override fun visitConstL(node: ConstL): ConstL = context(nodeBuilder, NoControlFlowBuilder) { ConstL(node.value) }

    override fun visitAddL(node: AddL): AddL = context(nodeBuilder, NoControlFlowBuilder) { AddL(null, null) } as AddL

    override fun visitSubL(node: SubL): SubL = context(nodeBuilder, NoControlFlowBuilder) { SubL(null, null) } as SubL

    override fun visitMulL(node: MulL): MulL = context(nodeBuilder, NoControlFlowBuilder) { MulL(null, null) } as MulL

    override fun visitDivL(node: DivL): DivL = context(nodeBuilder, NoControlFlowBuilder) { DivL(null, null) } as DivL

    override fun visitRemL(node: RemL): RemL = context(nodeBuilder, NoControlFlowBuilder) { RemL(null, null) } as RemL

    override fun visitConstF(node: ConstF): ConstF = context(nodeBuilder, NoControlFlowBuilder) { ConstF(node.value) }

    override fun visitAddF(node: AddF): AddF = context(nodeBuilder, NoControlFlowBuilder) { AddF(null, null) } as AddF

    override fun visitSubF(node: SubF): SubF = context(nodeBuilder, NoControlFlowBuilder) { SubF(null, null) } as SubF

    override fun visitMulF(node: MulF): MulF = context(nodeBuilder, NoControlFlowBuilder) { MulF(null, null) } as MulF

    override fun visitDivF(node: DivF): DivF = context(nodeBuilder, NoControlFlowBuilder) { DivF(null, null) } as DivF

    override fun visitRemF(node: RemF): RemF = context(nodeBuilder, NoControlFlowBuilder) { RemF(null, null) } as RemF

    override fun visitConstD(node: ConstD): ConstD = context(nodeBuilder, NoControlFlowBuilder) { ConstD(node.value) }

    override fun visitAddD(node: AddD): AddD = context(nodeBuilder, NoControlFlowBuilder) { AddD(null, null) } as AddD

    override fun visitSubD(node: SubD): SubD = context(nodeBuilder, NoControlFlowBuilder) { SubD(null, null) } as SubD

    override fun visitMulD(node: MulD): MulD = context(nodeBuilder, NoControlFlowBuilder) { MulD(null, null) } as MulD

    override fun visitDivD(node: DivD): DivD = context(nodeBuilder, NoControlFlowBuilder) { DivD(null, null) } as DivD

    override fun visitRemD(node: RemD): RemD = context(nodeBuilder, NoControlFlowBuilder) { RemD(null, null) } as RemD

    override fun visitNew(node: New): New = context(nodeBuilder, NoControlFlowBuilder) { New(node.type)(null) }

    override fun visitReadFieldPinned(node: ReadFieldPinned): ReadFieldPinned = context(nodeBuilder, NoControlFlowBuilder) { ReadFieldPinned(node.field)(null, null) }

    override fun visitReadGlobalPinned(node: ReadGlobalPinned): ReadGlobalPinned = context(nodeBuilder, NoControlFlowBuilder) { ReadGlobalPinned(node.field)(null) }

    override fun visitWriteField(node: WriteField): WriteField = context(nodeBuilder, NoControlFlowBuilder) { WriteField(node.field)(null, null, null) }

    override fun visitWriteGlobal(node: WriteGlobal): WriteGlobal = context(nodeBuilder, NoControlFlowBuilder) { WriteGlobal(node.field)(null, null) }

    override fun visitIsInstanceOf(node: IsInstanceOf): IsInstanceOf = context(nodeBuilder, NoControlFlowBuilder) { IsInstanceOf(node.type)(null) } as IsInstanceOf

    override fun visitCast(node: Cast): Cast = context(nodeBuilder, NoControlFlowBuilder) { Cast(node.type)(null) } as Cast

    override fun visitStaticCall(node: StaticCall): StaticCall = context(nodeBuilder, NoControlFlowBuilder) { StaticCall(node.function)(null, *Array(node.callArgs.size) { null }) }

}

