@file:Suppress("FunctionName")

package hair.ir
import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.*

context(nodeBuilder: NodeBuilder)
fun Use(control: Controlling?, value: Node?): Controlling = nodeBuilder.onNodeBuilt(Use(nodeBuilder.session.useForm, control, value)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Use(value: Node?): Controlling = controlBuilder.appendControlled { ctrl -> Use(ctrl, value) }

context(nodeBuilder: NodeBuilder)
fun NoValue(): NoValue = nodeBuilder.onNodeBuilt(NoValue(nodeBuilder.session.noValueForm)) as NoValue

context(nodeBuilder: NodeBuilder)
fun UnitValue(): UnitValue = nodeBuilder.onNodeBuilt(UnitValue(nodeBuilder.session.unitValueForm)) as UnitValue

context(nodeBuilder: NodeBuilder)
fun GlobalInit(control: Controlling?): Controlling = nodeBuilder.onNodeBuilt(GlobalInit(nodeBuilder.session.globalInitForm, control)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun GlobalInit(): Controlling = controlBuilder.appendControlled { ctrl -> GlobalInit(ctrl) }

context(nodeBuilder: NodeBuilder)
fun ThreadLocalInit(control: Controlling?): Controlling = nodeBuilder.onNodeBuilt(ThreadLocalInit(nodeBuilder.session.threadLocalInitForm, control)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ThreadLocalInit(): Controlling = controlBuilder.appendControlled { ctrl -> ThreadLocalInit(ctrl) }

context(nodeBuilder: NodeBuilder)
fun StandaloneThreadLocalInit(control: Controlling?): Controlling = nodeBuilder.onNodeBuilt(StandaloneThreadLocalInit(nodeBuilder.session.standaloneThreadLocalInitForm, control)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun StandaloneThreadLocalInit(): Controlling = controlBuilder.appendControlled { ctrl -> StandaloneThreadLocalInit(ctrl) }

context(nodeBuilder: NodeBuilder)
fun UnreachableNoCtrl(): Unreachable = nodeBuilder.onNodeBuilt(Unreachable(nodeBuilder.session.unreachableForm)) as Unreachable

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun Unreachable(): Unreachable = UnreachableNoCtrl()

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Unreachable(): Unreachable = controlBuilder.appendControl { UnreachableNoCtrl() }

context(nodeBuilder: NodeBuilder)
fun BlockEntryNoCtrl(vararg preds: BlockExit?): Controlling = nodeBuilder.onNodeBuilt(BlockEntry(nodeBuilder.session.blockEntryForm, *preds)) as Controlling

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun BlockEntry(vararg preds: BlockExit?): Controlling = BlockEntryNoCtrl(*preds)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun BlockEntry(vararg preds: BlockExit?): Controlling = controlBuilder.appendControl { BlockEntryNoCtrl(*preds) }

context(nodeBuilder: NodeBuilder)
fun Return(control: Controlling?, result: Node?): Node = nodeBuilder.onNodeBuilt(Return(nodeBuilder.session.returnForm, control, result))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Return(result: Node?): Node = controlBuilder.appendControlled { ctrl -> Return(ctrl, result) }

context(nodeBuilder: NodeBuilder)
fun Goto(control: Controlling?): BlockExit = nodeBuilder.onNodeBuilt(Goto(nodeBuilder.session.gotoForm, control)) as BlockExit

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Goto(): BlockExit = controlBuilder.appendControlled { ctrl -> Goto(ctrl) }

context(nodeBuilder: NodeBuilder)
fun If(control: Controlling?, cond: Node?): Node = nodeBuilder.onNodeBuilt(If(nodeBuilder.session.ifForm, control, cond))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun If(cond: Node?): Node = controlBuilder.appendControlled { ctrl -> If(ctrl, cond) }

context(nodeBuilder: NodeBuilder)
fun TrueExit(owner: If?): BlockExit = nodeBuilder.onNodeBuilt(TrueExit(nodeBuilder.session.trueExitForm, owner)) as BlockExit

context(nodeBuilder: NodeBuilder)
fun FalseExit(owner: If?): BlockExit = nodeBuilder.onNodeBuilt(FalseExit(nodeBuilder.session.falseExitForm, owner)) as BlockExit

context(nodeBuilder: NodeBuilder)
fun Throw(control: Controlling?, exception: Node?): Node = nodeBuilder.onNodeBuilt(Throw(nodeBuilder.session.throwForm, control, exception))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Throw(exception: Node?): Node = controlBuilder.appendControlled { ctrl -> Throw(ctrl, exception) }

context(nodeBuilder: NodeBuilder)
fun Unwind(thrower: Throwing?): BlockExit = nodeBuilder.onNodeBuilt(Unwind(nodeBuilder.session.unwindForm, thrower)) as BlockExit

context(nodeBuilder: NodeBuilder)
private fun ReadVarForm(variable: Any): ReadVar.Form = ReadVar.Form(nodeBuilder.session.readVarMetaForm, variable).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun ReadVar(variable: Any): ReadVar.Form = ReadVarForm(variable)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ReadVar(variable: Any): Controlling = ReadVarForm(variable)()

context(nodeBuilder: NodeBuilder)
operator fun ReadVar.Form.invoke(control: Controlling?): Controlling = nodeBuilder.onNodeBuilt(ReadVar(this@invoke, control)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun ReadVar.Form.invoke(): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun AssignVar(variable: Any): AssignVar.Form = AssignVar.Form(nodeBuilder.session.assignVarMetaForm, variable).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun AssignVar.Form.invoke(control: Controlling?, assignedValue: Node?): Controlling = nodeBuilder.onNodeBuilt(AssignVar(this@invoke, control, assignedValue)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun AssignVar.Form.invoke(assignedValue: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, assignedValue) }

context(nodeBuilder: NodeBuilder)
fun Phi(type: HairType): Phi.Form = Phi.Form(nodeBuilder.session.phiMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Phi.Form.invoke(block: BlockEntry?, vararg joinedValues: Node?): Node = nodeBuilder.onNodeBuilt(Phi(this@invoke, block, *joinedValues))

context(nodeBuilder: NodeBuilder)
fun PhiPlaceholder(origin: Any): PhiPlaceholder.Form = PhiPlaceholder.Form(nodeBuilder.session.phiPlaceholderMetaForm, origin).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun PhiPlaceholder.Form.invoke(block: BlockEntry?, vararg joinedValues: Node?): Node = nodeBuilder.onNodeBuilt(PhiPlaceholder(this@invoke, block, *joinedValues))

context(nodeBuilder: NodeBuilder)
private fun ParamForm(index: Int): Param.Form = Param.Form(nodeBuilder.session.paramMetaForm, index).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Param.Form.invoke(): Param = nodeBuilder.onNodeBuilt(Param(this@invoke)) as Param

context(nodeBuilder: NodeBuilder)
fun Param(index: Int): Param = ParamForm(index)()

context(nodeBuilder: NodeBuilder)
fun Catch(unwind: Node?): Node = nodeBuilder.onNodeBuilt(Catch(nodeBuilder.session.catchForm, unwind))

context(nodeBuilder: NodeBuilder)
private fun ConstIForm(value: Int): ConstI.Form = ConstI.Form(nodeBuilder.session.constIMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstI.Form.invoke(): ConstI = nodeBuilder.onNodeBuilt(ConstI(this@invoke)) as ConstI

context(nodeBuilder: NodeBuilder)
fun ConstI(value: Int): ConstI = ConstIForm(value)()

context(nodeBuilder: NodeBuilder)
private fun ConstLForm(value: Long): ConstL.Form = ConstL.Form(nodeBuilder.session.constLMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstL.Form.invoke(): ConstL = nodeBuilder.onNodeBuilt(ConstL(this@invoke)) as ConstL

context(nodeBuilder: NodeBuilder)
fun ConstL(value: Long): ConstL = ConstLForm(value)()

context(nodeBuilder: NodeBuilder)
private fun ConstFForm(value: Float): ConstF.Form = ConstF.Form(nodeBuilder.session.constFMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstF.Form.invoke(): ConstF = nodeBuilder.onNodeBuilt(ConstF(this@invoke)) as ConstF

context(nodeBuilder: NodeBuilder)
fun ConstF(value: Float): ConstF = ConstFForm(value)()

context(nodeBuilder: NodeBuilder)
private fun ConstDForm(value: Double): ConstD.Form = ConstD.Form(nodeBuilder.session.constDMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstD.Form.invoke(): ConstD = nodeBuilder.onNodeBuilt(ConstD(this@invoke)) as ConstD

context(nodeBuilder: NodeBuilder)
fun ConstD(value: Double): ConstD = ConstDForm(value)()

context(nodeBuilder: NodeBuilder)
fun True(): True = nodeBuilder.onNodeBuilt(True(nodeBuilder.session.trueForm)) as True

context(nodeBuilder: NodeBuilder)
fun False(): False = nodeBuilder.onNodeBuilt(False(nodeBuilder.session.falseForm)) as False

context(nodeBuilder: NodeBuilder)
fun Null(): Null = nodeBuilder.onNodeBuilt(Null(nodeBuilder.session.nullForm)) as Null

context(nodeBuilder: NodeBuilder)
fun Add(type: HairType): Add.Form = Add.Form(nodeBuilder.session.addMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Add.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Add(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Sub(type: HairType): Sub.Form = Sub.Form(nodeBuilder.session.subMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Sub.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Sub(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Mul(type: HairType): Mul.Form = Mul.Form(nodeBuilder.session.mulMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Mul.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Mul(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Div(type: HairType): Div.Form = Div.Form(nodeBuilder.session.divMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Div.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Div(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Rem(type: HairType): Rem.Form = Rem.Form(nodeBuilder.session.remMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Rem.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Rem(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun And(type: HairType): And.Form = And.Form(nodeBuilder.session.andMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun And.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(And(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Or(type: HairType): Or.Form = Or.Form(nodeBuilder.session.orMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Or.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Or(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Xor(type: HairType): Xor.Form = Xor.Form(nodeBuilder.session.xorMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Xor.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Xor(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Shl(type: HairType): Shl.Form = Shl.Form(nodeBuilder.session.shlMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Shl.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Shl(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Shr(type: HairType): Shr.Form = Shr.Form(nodeBuilder.session.shrMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Shr.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Shr(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Ushr(type: HairType): Ushr.Form = Ushr.Form(nodeBuilder.session.ushrMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Ushr.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Ushr(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Cmp(type: HairType, op: CmpOp): Cmp.Form = Cmp.Form(nodeBuilder.session.cmpMetaForm, type, op).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Cmp.Form.invoke(lhs: Node?, rhs: Node?): Node = nodeBuilder.onNodeBuilt(Cmp(this@invoke, lhs, rhs))

context(nodeBuilder: NodeBuilder)
fun Not(operand: Node?): Node = nodeBuilder.onNodeBuilt(Not(nodeBuilder.session.notForm, operand))

context(nodeBuilder: NodeBuilder)
fun SignExtend(targetType: HairType): SignExtend.Form = SignExtend.Form(nodeBuilder.session.signExtendMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun SignExtend.Form.invoke(operand: Node?): Node = nodeBuilder.onNodeBuilt(SignExtend(this@invoke, operand))

context(nodeBuilder: NodeBuilder)
fun ZeroExtend(targetType: HairType): ZeroExtend.Form = ZeroExtend.Form(nodeBuilder.session.zeroExtendMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ZeroExtend.Form.invoke(operand: Node?): Node = nodeBuilder.onNodeBuilt(ZeroExtend(this@invoke, operand))

context(nodeBuilder: NodeBuilder)
fun Truncate(targetType: HairType): Truncate.Form = Truncate.Form(nodeBuilder.session.truncateMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Truncate.Form.invoke(operand: Node?): Node = nodeBuilder.onNodeBuilt(Truncate(this@invoke, operand))

context(nodeBuilder: NodeBuilder)
fun Reinterpret(targetType: HairType): Reinterpret.Form = Reinterpret.Form(nodeBuilder.session.reinterpretMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Reinterpret.Form.invoke(operand: Node?): Node = nodeBuilder.onNodeBuilt(Reinterpret(this@invoke, operand))

context(nodeBuilder: NodeBuilder)
private fun NewForm(objectType: HairClass): New.Form = New.Form(nodeBuilder.session.newMetaForm, objectType).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun New(objectType: HairClass): New.Form = NewForm(objectType)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun New(objectType: HairClass): Controlling = NewForm(objectType)()

context(nodeBuilder: NodeBuilder)
operator fun New.Form.invoke(control: Controlling?): Controlling = nodeBuilder.onNodeBuilt(New(this@invoke, control)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun New.Form.invoke(): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun NewArray(elementType: HairClass): NewArray.Form = NewArray.Form(nodeBuilder.session.newArrayMetaForm, elementType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun NewArray.Form.invoke(control: Controlling?, size: Node?): Controlling = nodeBuilder.onNodeBuilt(NewArray(this@invoke, control, size)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun NewArray.Form.invoke(size: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, size) }

context(nodeBuilder: NodeBuilder)
fun IsInstanceOf(targetType: HairClass): IsInstanceOf.Form = IsInstanceOf.Form(nodeBuilder.session.isInstanceOfMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun IsInstanceOf.Form.invoke(obj: Node?): Node = nodeBuilder.onNodeBuilt(IsInstanceOf(this@invoke, obj))

context(nodeBuilder: NodeBuilder)
fun CheckCast(targetType: HairClass): CheckCast.Form = CheckCast.Form(nodeBuilder.session.checkCastMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun CheckCast.Form.invoke(obj: Node?): Node = nodeBuilder.onNodeBuilt(CheckCast(this@invoke, obj))

context(nodeBuilder: NodeBuilder)
fun Load(type: HairType): Load.Form = Load.Form(nodeBuilder.session.loadMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Load.Form.invoke(location: Node?): Node = nodeBuilder.onNodeBuilt(Load(this@invoke, location))

context(nodeBuilder: NodeBuilder)
fun Store(type: HairType): Store.Form = Store.Form(nodeBuilder.session.storeMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Store.Form.invoke(location: Node?): Node = nodeBuilder.onNodeBuilt(Store(this@invoke, location))

context(nodeBuilder: NodeBuilder)
fun LoadField(field: Field): LoadField.Form = LoadField.Form(nodeBuilder.session.loadFieldMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun LoadField.Form.invoke(obj: Node?): Node = nodeBuilder.onNodeBuilt(LoadField(this@invoke, obj))

context(nodeBuilder: NodeBuilder)
fun StoreField(field: Field): StoreField.Form = StoreField.Form(nodeBuilder.session.storeFieldMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun StoreField.Form.invoke(obj: Node?, value: Node?): Node = nodeBuilder.onNodeBuilt(StoreField(this@invoke, obj, value))

context(nodeBuilder: NodeBuilder)
private fun LoadGlobalForm(field: Global): LoadGlobal.Form = LoadGlobal.Form(nodeBuilder.session.loadGlobalMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun LoadGlobal.Form.invoke(): LoadGlobal = nodeBuilder.onNodeBuilt(LoadGlobal(this@invoke)) as LoadGlobal

context(nodeBuilder: NodeBuilder)
fun LoadGlobal(field: Global): LoadGlobal = LoadGlobalForm(field)()

context(nodeBuilder: NodeBuilder)
fun StoreGlobal(field: Global): StoreGlobal.Form = StoreGlobal.Form(nodeBuilder.session.storeGlobalMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun StoreGlobal.Form.invoke(value: Node?): Node = nodeBuilder.onNodeBuilt(StoreGlobal(this@invoke, value))

context(nodeBuilder: NodeBuilder)
fun InvokeStatic(function: HairFunction): InvokeStatic.Form = InvokeStatic.Form(nodeBuilder.session.invokeStaticMetaForm, function).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun InvokeStatic.Form.invoke(control: Controlling?, vararg callArgs: Node?): Controlling = nodeBuilder.onNodeBuilt(InvokeStatic(this@invoke, control, *callArgs)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun InvokeStatic.Form.invoke(vararg callArgs: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, *callArgs) }

context(nodeBuilder: NodeBuilder)
fun InvokeVirtual(function: HairFunction): InvokeVirtual.Form = InvokeVirtual.Form(nodeBuilder.session.invokeVirtualMetaForm, function).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun InvokeVirtual.Form.invoke(control: Controlling?, vararg callArgs: Node?): Controlling = nodeBuilder.onNodeBuilt(InvokeVirtual(this@invoke, control, *callArgs)) as Controlling

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun InvokeVirtual.Form.invoke(vararg callArgs: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, *callArgs) }

