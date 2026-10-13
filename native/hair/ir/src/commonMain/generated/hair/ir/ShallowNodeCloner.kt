package hair.ir
import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.*

class ShallowNodeCloner(val nodeBuilder: NodeBuilder): NodeVisitor<Node>() {
    override fun visitNode(node: Node): Node = error("Should not reach here $node")

    override fun visitUse(node: Use): Use = context(nodeBuilder) { Use(null, null) } as Use

    override fun visitNoValue(node: NoValue): NoValue = context(nodeBuilder) { NoValue() }

    override fun visitUnitValue(node: UnitValue): UnitValue = context(nodeBuilder) { UnitValue() }

    override fun visitGlobalInit(node: GlobalInit): GlobalInit = context(nodeBuilder) { GlobalInit(node.initRoutine)(null) } as GlobalInit

    override fun visitThreadLocalInit(node: ThreadLocalInit): ThreadLocalInit = context(nodeBuilder) { ThreadLocalInit(node.initRoutine)(null) } as ThreadLocalInit

    override fun visitStandaloneThreadLocalInit(node: StandaloneThreadLocalInit): StandaloneThreadLocalInit = context(nodeBuilder) { StandaloneThreadLocalInit(node.initRoutine)(null) } as StandaloneThreadLocalInit

    override fun visitUnreachable(node: Unreachable): Unreachable = context(nodeBuilder) { Unreachable() }

    override fun visitBlockEntry(node: BlockEntry): BlockEntry = context(nodeBuilder) { BlockEntry(*Array(node.preds.size) { null }) } as BlockEntry

    override fun visitHalt(node: Halt): Halt = context(nodeBuilder) { Halt(null) } as Halt

    override fun visitReturn(node: Return): Return = context(nodeBuilder) { Return(null, null) } as Return

    override fun visitGoto(node: Goto): Goto = context(nodeBuilder) { Goto(null) } as Goto

    override fun visitIf(node: If): If = context(nodeBuilder) { If(null, null) } as If

    override fun visitTrueExit(node: TrueExit): TrueExit = context(nodeBuilder) { TrueExit(null) } as TrueExit

    override fun visitFalseExit(node: FalseExit): FalseExit = context(nodeBuilder) { FalseExit(null) } as FalseExit

    override fun visitThrow(node: Throw): Throw = context(nodeBuilder) { Throw(null, null) } as Throw

    override fun visitUnwind(node: Unwind): Unwind = context(nodeBuilder) { Unwind(null) } as Unwind

    override fun visitReadVar(node: ReadVar): ReadVar = context(nodeBuilder) { ReadVar(node.variable)(null) } as ReadVar

    override fun visitAssignVar(node: AssignVar): AssignVar = context(nodeBuilder) { AssignVar(node.variable)(null, null) } as AssignVar

    override fun visitPhi(node: Phi): Phi = context(nodeBuilder) { Phi(null, *Array(node.joinedValues.size) { null }) } as Phi

    override fun visitPhiPlaceholder(node: PhiPlaceholder): PhiPlaceholder = context(nodeBuilder) { PhiPlaceholder(node.origin)(null, *Array(node.joinedValues.size) { null }) } as PhiPlaceholder

    override fun visitParam(node: Param): Param = context(nodeBuilder) { Param(node.index) }

    override fun visitCatch(node: Catch): Catch = context(nodeBuilder) { Catch(null) } as Catch

    override fun visitConst(node: Const): Const = context(nodeBuilder) { Const(node.value) }

    override fun visitNull(node: Null): Null = context(nodeBuilder) { Null() }

    override fun visitTrue(node: True): True = context(nodeBuilder) { True() }

    override fun visitFalse(node: False): False = context(nodeBuilder) { False() }

    override fun visitAdd(node: Add): Add = context(nodeBuilder) { Add(node.opType)(null, null) } as Add

    override fun visitSub(node: Sub): Sub = context(nodeBuilder) { Sub(node.opType)(null, null) } as Sub

    override fun visitMul(node: Mul): Mul = context(nodeBuilder) { Mul(node.opType)(null, null) } as Mul

    override fun visitDiv(node: Div): Div = context(nodeBuilder) { Div(node.opType)(null, null) } as Div

    override fun visitRem(node: Rem): Rem = context(nodeBuilder) { Rem(node.opType)(null, null) } as Rem

    override fun visitNeg(node: Neg): Neg = context(nodeBuilder) { Neg(null) } as Neg

    override fun visitAnd(node: And): And = context(nodeBuilder) { And(node.opType)(null, null) } as And

    override fun visitOr(node: Or): Or = context(nodeBuilder) { Or(node.opType)(null, null) } as Or

    override fun visitXor(node: Xor): Xor = context(nodeBuilder) { Xor(node.opType)(null, null) } as Xor

    override fun visitShl(node: Shl): Shl = context(nodeBuilder) { Shl(node.opType)(null, null) } as Shl

    override fun visitShr(node: Shr): Shr = context(nodeBuilder) { Shr(node.opType)(null, null) } as Shr

    override fun visitUshr(node: Ushr): Ushr = context(nodeBuilder) { Ushr(node.opType)(null, null) } as Ushr

    override fun visitInv(node: Inv): Inv = context(nodeBuilder) { Inv(null) } as Inv

    override fun visitCmp(node: Cmp): Cmp = context(nodeBuilder) { Cmp(node.type, node.op)(null, null) } as Cmp

    override fun visitNot(node: Not): Not = context(nodeBuilder) { Not(null) } as Not

    override fun visitSignExtend(node: SignExtend): SignExtend = context(nodeBuilder) { SignExtend(node.targetType)(null) } as SignExtend

    override fun visitZeroExtend(node: ZeroExtend): ZeroExtend = context(nodeBuilder) { ZeroExtend(node.targetType)(null) } as ZeroExtend

    override fun visitTruncate(node: Truncate): Truncate = context(nodeBuilder) { Truncate(node.targetType)(null) } as Truncate

    override fun visitReinterpret(node: Reinterpret): Reinterpret = context(nodeBuilder) { Reinterpret(node.targetType)(null) } as Reinterpret

    override fun visitNew(node: New): New = context(nodeBuilder) { New(node.objectType)(null) } as New

    override fun visitNewArray(node: NewArray): NewArray = context(nodeBuilder) { NewArray(node.elementType)(null, null) } as NewArray

    override fun visitIsInstanceOf(node: IsInstanceOf): IsInstanceOf = context(nodeBuilder) { IsInstanceOf(node.targetType)(null) } as IsInstanceOf

    override fun visitCheckCast(node: CheckCast): CheckCast = context(nodeBuilder) { CheckCast(node.targetType)(null, null) } as CheckCast

    override fun visitTypeInfo(node: TypeInfo): TypeInfo = context(nodeBuilder) { TypeInfo(null) } as TypeInfo

    override fun visitConstTypeInfo(node: ConstTypeInfo): ConstTypeInfo = context(nodeBuilder) { ConstTypeInfo(node.type) }

    override fun visitArraySize(node: ArraySize): ArraySize = context(nodeBuilder) { ArraySize(null) } as ArraySize

    override fun visitArrayIndexCheck(node: ArrayIndexCheck): ArrayIndexCheck = context(nodeBuilder) { ArrayIndexCheck(null, null, null) } as ArrayIndexCheck

    override fun visitLoad(node: Load): Load = context(nodeBuilder) { Load(node.type)(null, null) } as Load

    override fun visitStore(node: Store): Store = context(nodeBuilder) { Store(node.type)(null, null, null) } as Store

    override fun visitLoadField(node: LoadField): LoadField = context(nodeBuilder) { LoadField(node.field)(null, null) } as LoadField

    override fun visitStoreField(node: StoreField): StoreField = context(nodeBuilder) { StoreField(node.field)(null, null, null) } as StoreField

    override fun visitLoadGlobal(node: LoadGlobal): LoadGlobal = context(nodeBuilder) { LoadGlobal(node.field)(null) } as LoadGlobal

    override fun visitStoreGlobal(node: StoreGlobal): StoreGlobal = context(nodeBuilder) { StoreGlobal(node.field)(null, null) } as StoreGlobal

    override fun visitLoadArrayElement(node: LoadArrayElement): LoadArrayElement = context(nodeBuilder) { LoadArrayElement(node.elementType)(null, null, null) } as LoadArrayElement

    override fun visitStoreArrayElement(node: StoreArrayElement): StoreArrayElement = context(nodeBuilder) { StoreArrayElement(node.elementType)(null, null, null, null) } as StoreArrayElement

    override fun visitInvokeStatic(node: InvokeStatic): InvokeStatic = context(nodeBuilder) { InvokeStatic(node.function)(null, *Array(node.callArgs.size) { null }) } as InvokeStatic

    override fun visitInvokeVirtual(node: InvokeVirtual): InvokeVirtual = context(nodeBuilder) { InvokeVirtual(node.function)(null, *Array(node.callArgs.size) { null }) } as InvokeVirtual

}

