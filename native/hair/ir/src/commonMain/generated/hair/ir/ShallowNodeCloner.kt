package hair.ir
import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.*

class ShallowNodeCloner(val nodeBuilder: NodeBuilder): NodeVisitor<Node>() {
    override fun visitNode(node: Node): Node = error("Should not reach here $node")

    override fun visitNoValue(node: NoValue): NoValue = context(nodeBuilder, NoControlFlowBuilder) { NoValue() }

    override fun visitUnitValue(node: UnitValue): UnitValue = context(nodeBuilder, NoControlFlowBuilder) { UnitValue() }

    override fun visitUse(node: Use): Use = context(nodeBuilder, NoControlFlowBuilder) { Use(null, null) } as Use

    override fun visitUnreachable(node: Unreachable): Unreachable = context(nodeBuilder, NoControlFlowBuilder) { Unreachable() }

    override fun visitBlockEntry(node: BlockEntry): BlockEntry = context(nodeBuilder, NoControlFlowBuilder) { BlockEntry(*Array(node.preds.size) { null }) } as BlockEntry

    override fun visitReturn(node: Return): Return = context(nodeBuilder, NoControlFlowBuilder) { Return(null, null) } as Return

    override fun visitGoto(node: Goto): Goto = context(nodeBuilder, NoControlFlowBuilder) { Goto(null) } as Goto

    override fun visitIf(node: If): If = context(nodeBuilder, NoControlFlowBuilder) { If(null, null) } as If

    override fun visitTrueExit(node: TrueExit): TrueExit = context(nodeBuilder, NoControlFlowBuilder) { TrueExit(null) } as TrueExit

    override fun visitFalseExit(node: FalseExit): FalseExit = context(nodeBuilder, NoControlFlowBuilder) { FalseExit(null) } as FalseExit

    override fun visitThrow(node: Throw): Throw = context(nodeBuilder, NoControlFlowBuilder) { Throw(null, null) } as Throw

    override fun visitUnwind(node: Unwind): Unwind = context(nodeBuilder, NoControlFlowBuilder) { Unwind(null) } as Unwind

    override fun visitReadVar(node: ReadVar): ReadVar = context(nodeBuilder, NoControlFlowBuilder) { ReadVar(node.variable)(null) } as ReadVar

    override fun visitAssignVar(node: AssignVar): AssignVar = context(nodeBuilder, NoControlFlowBuilder) { AssignVar(node.variable)(null, null) } as AssignVar

    override fun visitPhi(node: Phi): Phi = context(nodeBuilder, NoControlFlowBuilder) { Phi(node.type)(null, *Array(node.joinedValues.size) { null }) } as Phi

    override fun visitPhiPlaceholder(node: PhiPlaceholder): PhiPlaceholder = context(nodeBuilder, NoControlFlowBuilder) { PhiPlaceholder(node.origin)(null, *Array(node.joinedValues.size) { null }) } as PhiPlaceholder

    override fun visitParam(node: Param): Param = context(nodeBuilder, NoControlFlowBuilder) { Param(node.index) }

    override fun visitCatch(node: Catch): Catch = context(nodeBuilder, NoControlFlowBuilder) { Catch(null) } as Catch

    override fun visitConstI(node: ConstI): ConstI = context(nodeBuilder, NoControlFlowBuilder) { ConstI(node.value) }

    override fun visitConstL(node: ConstL): ConstL = context(nodeBuilder, NoControlFlowBuilder) { ConstL(node.value) }

    override fun visitConstF(node: ConstF): ConstF = context(nodeBuilder, NoControlFlowBuilder) { ConstF(node.value) }

    override fun visitConstD(node: ConstD): ConstD = context(nodeBuilder, NoControlFlowBuilder) { ConstD(node.value) }

    override fun visitTrue(node: True): True = context(nodeBuilder, NoControlFlowBuilder) { True() }

    override fun visitFalse(node: False): False = context(nodeBuilder, NoControlFlowBuilder) { False() }

    override fun visitNull(node: Null): Null = context(nodeBuilder, NoControlFlowBuilder) { Null() }

    override fun visitAdd(node: Add): Add = context(nodeBuilder, NoControlFlowBuilder) { Add(node.type)(null, null) } as Add

    override fun visitSub(node: Sub): Sub = context(nodeBuilder, NoControlFlowBuilder) { Sub(node.type)(null, null) } as Sub

    override fun visitMul(node: Mul): Mul = context(nodeBuilder, NoControlFlowBuilder) { Mul(node.type)(null, null) } as Mul

    override fun visitDiv(node: Div): Div = context(nodeBuilder, NoControlFlowBuilder) { Div(node.type)(null, null) } as Div

    override fun visitRem(node: Rem): Rem = context(nodeBuilder, NoControlFlowBuilder) { Rem(node.type)(null, null) } as Rem

    override fun visitAnd(node: And): And = context(nodeBuilder, NoControlFlowBuilder) { And(node.type)(null, null) } as And

    override fun visitOr(node: Or): Or = context(nodeBuilder, NoControlFlowBuilder) { Or(node.type)(null, null) } as Or

    override fun visitXor(node: Xor): Xor = context(nodeBuilder, NoControlFlowBuilder) { Xor(node.type)(null, null) } as Xor

    override fun visitShl(node: Shl): Shl = context(nodeBuilder, NoControlFlowBuilder) { Shl(node.type)(null, null) } as Shl

    override fun visitShr(node: Shr): Shr = context(nodeBuilder, NoControlFlowBuilder) { Shr(node.type)(null, null) } as Shr

    override fun visitUshr(node: Ushr): Ushr = context(nodeBuilder, NoControlFlowBuilder) { Ushr(node.type)(null, null) } as Ushr

    override fun visitCmp(node: Cmp): Cmp = context(nodeBuilder, NoControlFlowBuilder) { Cmp(node.type, node.op)(null, null) } as Cmp

    override fun visitNew(node: New): New = context(nodeBuilder, NoControlFlowBuilder) { New(node.objectType)(null) } as New

    override fun visitReadFieldPinned(node: ReadFieldPinned): ReadFieldPinned = context(nodeBuilder, NoControlFlowBuilder) { ReadFieldPinned(node.field)(null, null) } as ReadFieldPinned

    override fun visitReadGlobalPinned(node: ReadGlobalPinned): ReadGlobalPinned = context(nodeBuilder, NoControlFlowBuilder) { ReadGlobalPinned(node.field)(null) } as ReadGlobalPinned

    override fun visitWriteField(node: WriteField): WriteField = context(nodeBuilder, NoControlFlowBuilder) { WriteField(node.field)(null, null, null) } as WriteField

    override fun visitWriteGlobal(node: WriteGlobal): WriteGlobal = context(nodeBuilder, NoControlFlowBuilder) { WriteGlobal(node.field)(null, null) } as WriteGlobal

    override fun visitIsInstanceOf(node: IsInstanceOf): IsInstanceOf = context(nodeBuilder, NoControlFlowBuilder) { IsInstanceOf(node.targetType)(null) } as IsInstanceOf

    override fun visitCheckCast(node: CheckCast): CheckCast = context(nodeBuilder, NoControlFlowBuilder) { CheckCast(node.targetType)(null) } as CheckCast

    override fun visitInvokeStatic(node: InvokeStatic): InvokeStatic = context(nodeBuilder, NoControlFlowBuilder) { InvokeStatic(node.function)(null, *Array(node.callArgs.size) { null }) } as InvokeStatic

    override fun visitInvokeVirtual(node: InvokeVirtual): InvokeVirtual = context(nodeBuilder, NoControlFlowBuilder) { InvokeVirtual(node.function)(null, *Array(node.callArgs.size) { null }) } as InvokeVirtual

}

