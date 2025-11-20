@file:Suppress("FunctionName")

package hair.ir
import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.*

context(nodeBuilder: NodeBuilder)
fun NoValue(): NoValue = nodeBuilder.register(NoValue(nodeBuilder.session.noValueForm))

context(nodeBuilder: NodeBuilder)
fun UnitValue(): UnitValue = nodeBuilder.register(UnitValue(nodeBuilder.session.unitValueForm))

context(nodeBuilder: NodeBuilder)
fun Use(control: Controlling?, value: Node?): Controlling = (nodeBuilder.normalize(Use(nodeBuilder.session.useForm, control, value)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Use(value: Node?): Controlling = controlBuilder.appendControlled { ctrl -> Use(ctrl, value) }

context(nodeBuilder: NodeBuilder)
fun UnreachableNoCtrl(): Unreachable = nodeBuilder.register(Unreachable(nodeBuilder.session.unreachableForm))

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun Unreachable(): Unreachable = UnreachableNoCtrl()

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Unreachable(): Unreachable = controlBuilder.appendControl { UnreachableNoCtrl() }

context(nodeBuilder: NodeBuilder)
fun BlockEntryNoCtrl(vararg preds: BlockExit?): Controlling = (nodeBuilder.normalize(BlockEntry(nodeBuilder.session.blockEntryForm, *preds)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun BlockEntry(vararg preds: BlockExit?): Controlling = BlockEntryNoCtrl(*preds)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun BlockEntry(vararg preds: BlockExit?): Controlling = controlBuilder.appendControl { BlockEntryNoCtrl(*preds) }

context(nodeBuilder: NodeBuilder)
fun Return(control: Controlling?, result: Node?): Node = (nodeBuilder.normalize(Return(nodeBuilder.session.returnForm, control, result))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Return(result: Node?): Node = controlBuilder.appendControlled { ctrl -> Return(ctrl, result) }

context(nodeBuilder: NodeBuilder)
fun Goto(control: Controlling?): BlockExit = (nodeBuilder.normalize(Goto(nodeBuilder.session.gotoForm, control)) as BlockExit).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Goto(): BlockExit = controlBuilder.appendControlled { ctrl -> Goto(ctrl) }

context(nodeBuilder: NodeBuilder)
fun If(control: Controlling?, cond: Node?): Node = (nodeBuilder.normalize(If(nodeBuilder.session.ifForm, control, cond))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun If(cond: Node?): Node = controlBuilder.appendControlled { ctrl -> If(ctrl, cond) }

context(nodeBuilder: NodeBuilder)
fun TrueExit(owner: If?): BlockExit = (nodeBuilder.normalize(TrueExit(nodeBuilder.session.trueExitForm, owner)) as BlockExit).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun FalseExit(owner: If?): BlockExit = (nodeBuilder.normalize(FalseExit(nodeBuilder.session.falseExitForm, owner)) as BlockExit).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Throw(control: Controlling?, exception: Node?): Node = (nodeBuilder.normalize(Throw(nodeBuilder.session.throwForm, control, exception))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Throw(exception: Node?): Node = controlBuilder.appendControlled { ctrl -> Throw(ctrl, exception) }

context(nodeBuilder: NodeBuilder)
fun Unwind(thrower: Throwing?): BlockExit = (nodeBuilder.normalize(Unwind(nodeBuilder.session.unwindForm, thrower)) as BlockExit).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ReadVarForm(variable: Any): ReadVar.Form = ReadVar.Form(nodeBuilder.session.readVarMetaForm, variable).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun ReadVar(variable: Any): ReadVar.Form = ReadVarForm(variable)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ReadVar(variable: Any): Controlling = ReadVarForm(variable)()

context(nodeBuilder: NodeBuilder)
operator fun ReadVar.Form.invoke(control: Controlling?): Controlling = (nodeBuilder.normalize(ReadVar(this@invoke, control)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun ReadVar.Form.invoke(): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun AssignVar(variable: Any): AssignVar.Form = AssignVar.Form(nodeBuilder.session.assignVarMetaForm, variable).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun AssignVar.Form.invoke(control: Controlling?, assignedValue: Node?): Controlling = (nodeBuilder.normalize(AssignVar(this@invoke, control, assignedValue)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun AssignVar.Form.invoke(assignedValue: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, assignedValue) }

context(nodeBuilder: NodeBuilder)
fun Phi(type: HairType): Phi.Form = Phi.Form(nodeBuilder.session.phiMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Phi.Form.invoke(block: BlockEntry?, vararg joinedValues: Node?): Node = (nodeBuilder.normalize(Phi(this@invoke, block, *joinedValues))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun PhiPlaceholder(origin: Any): PhiPlaceholder.Form = PhiPlaceholder.Form(nodeBuilder.session.phiPlaceholderMetaForm, origin).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun PhiPlaceholder.Form.invoke(block: BlockEntry?, vararg joinedValues: Node?): Node = (nodeBuilder.normalize(PhiPlaceholder(this@invoke, block, *joinedValues))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ParamForm(index: Int): Param.Form = Param.Form(nodeBuilder.session.paramMetaForm, index).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Param.Form.invoke(): Param = nodeBuilder.register(Param(this@invoke))

context(nodeBuilder: NodeBuilder)
fun Param(index: Int): Param = ParamForm(index)()

context(nodeBuilder: NodeBuilder)
fun Catch(unwind: Node?): Node = (nodeBuilder.normalize(Catch(nodeBuilder.session.catchForm, unwind))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ConstIForm(value: Int): ConstI.Form = ConstI.Form(nodeBuilder.session.constIMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstI.Form.invoke(): ConstI = nodeBuilder.register(ConstI(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstI(value: Int): ConstI = ConstIForm(value)()

context(nodeBuilder: NodeBuilder)
private fun ConstLForm(value: Long): ConstL.Form = ConstL.Form(nodeBuilder.session.constLMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstL.Form.invoke(): ConstL = nodeBuilder.register(ConstL(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstL(value: Long): ConstL = ConstLForm(value)()

context(nodeBuilder: NodeBuilder)
private fun ConstFForm(value: Float): ConstF.Form = ConstF.Form(nodeBuilder.session.constFMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstF.Form.invoke(): ConstF = nodeBuilder.register(ConstF(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstF(value: Float): ConstF = ConstFForm(value)()

context(nodeBuilder: NodeBuilder)
private fun ConstDForm(value: Double): ConstD.Form = ConstD.Form(nodeBuilder.session.constDMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstD.Form.invoke(): ConstD = nodeBuilder.register(ConstD(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstD(value: Double): ConstD = ConstDForm(value)()

context(nodeBuilder: NodeBuilder)
fun True(): True = nodeBuilder.register(True(nodeBuilder.session.trueForm))

context(nodeBuilder: NodeBuilder)
fun False(): False = nodeBuilder.register(False(nodeBuilder.session.falseForm))

context(nodeBuilder: NodeBuilder)
fun Null(): Null = nodeBuilder.register(Null(nodeBuilder.session.nullForm))

context(nodeBuilder: NodeBuilder)
fun Add(type: HairType): Add.Form = Add.Form(nodeBuilder.session.addMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Add.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Add(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Sub(type: HairType): Sub.Form = Sub.Form(nodeBuilder.session.subMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Sub.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Sub(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Mul(type: HairType): Mul.Form = Mul.Form(nodeBuilder.session.mulMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Mul.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Mul(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Div(type: HairType): Div.Form = Div.Form(nodeBuilder.session.divMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Div.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Div(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Rem(type: HairType): Rem.Form = Rem.Form(nodeBuilder.session.remMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Rem.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Rem(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun And(type: HairType): And.Form = And.Form(nodeBuilder.session.andMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun And.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(And(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Or(type: HairType): Or.Form = Or.Form(nodeBuilder.session.orMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Or.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Or(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Xor(type: HairType): Xor.Form = Xor.Form(nodeBuilder.session.xorMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Xor.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Xor(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Shl(type: HairType): Shl.Form = Shl.Form(nodeBuilder.session.shlMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Shl.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Shl(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Shr(type: HairType): Shr.Form = Shr.Form(nodeBuilder.session.shrMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Shr.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Shr(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Ushr(type: HairType): Ushr.Form = Ushr.Form(nodeBuilder.session.ushrMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Ushr.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Ushr(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Cmp(type: HairType, op: CmpOp): Cmp.Form = Cmp.Form(nodeBuilder.session.cmpMetaForm, type, op).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Cmp.Form.invoke(lhs: Node?, rhs: Node?): Node = (nodeBuilder.normalize(Cmp(this@invoke, lhs, rhs))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun NewForm(objectType: Class): New.Form = New.Form(nodeBuilder.session.newMetaForm, objectType).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun New(objectType: Class): New.Form = NewForm(objectType)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun New(objectType: Class): Controlling = NewForm(objectType)()

context(nodeBuilder: NodeBuilder)
operator fun New.Form.invoke(control: Controlling?): Controlling = (nodeBuilder.normalize(New(this@invoke, control)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun New.Form.invoke(): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun ReadFieldPinned(field: Field): ReadFieldPinned.Form = ReadFieldPinned.Form(nodeBuilder.session.readFieldPinnedMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ReadFieldPinned.Form.invoke(control: Controlling?, obj: Node?): Controlling = (nodeBuilder.normalize(ReadFieldPinned(this@invoke, control, obj)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun ReadFieldPinned.Form.invoke(obj: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, obj) }

context(nodeBuilder: NodeBuilder)
private fun ReadGlobalPinnedForm(field: Global): ReadGlobalPinned.Form = ReadGlobalPinned.Form(nodeBuilder.session.readGlobalPinnedMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun ReadGlobalPinned(field: Global): ReadGlobalPinned.Form = ReadGlobalPinnedForm(field)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ReadGlobalPinned(field: Global): Controlling = ReadGlobalPinnedForm(field)()

context(nodeBuilder: NodeBuilder)
operator fun ReadGlobalPinned.Form.invoke(control: Controlling?): Controlling = (nodeBuilder.normalize(ReadGlobalPinned(this@invoke, control)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun ReadGlobalPinned.Form.invoke(): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun WriteField(field: Field): WriteField.Form = WriteField.Form(nodeBuilder.session.writeFieldMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun WriteField.Form.invoke(control: Controlling?, obj: Node?, value: Node?): Controlling = (nodeBuilder.normalize(WriteField(this@invoke, control, obj, value)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun WriteField.Form.invoke(obj: Node?, value: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, obj, value) }

context(nodeBuilder: NodeBuilder)
fun WriteGlobal(field: Global): WriteGlobal.Form = WriteGlobal.Form(nodeBuilder.session.writeGlobalMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun WriteGlobal.Form.invoke(control: Controlling?, value: Node?): Controlling = (nodeBuilder.normalize(WriteGlobal(this@invoke, control, value)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun WriteGlobal.Form.invoke(value: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, value) }

context(nodeBuilder: NodeBuilder)
fun IsInstanceOf(targetType: Reference): IsInstanceOf.Form = IsInstanceOf.Form(nodeBuilder.session.isInstanceOfMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun IsInstanceOf.Form.invoke(obj: Node?): Node = (nodeBuilder.normalize(IsInstanceOf(this@invoke, obj))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun CheckCast(targetType: Reference): CheckCast.Form = CheckCast.Form(nodeBuilder.session.checkCastMetaForm, targetType).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun CheckCast.Form.invoke(obj: Node?): Node = (nodeBuilder.normalize(CheckCast(this@invoke, obj))).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun InvokeStatic(function: HairFunction): InvokeStatic.Form = InvokeStatic.Form(nodeBuilder.session.invokeStaticMetaForm, function).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun InvokeStatic.Form.invoke(control: Controlling?, vararg callArgs: Node?): Controlling = (nodeBuilder.normalize(InvokeStatic(this@invoke, control, *callArgs)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun InvokeStatic.Form.invoke(vararg callArgs: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, *callArgs) }

context(nodeBuilder: NodeBuilder)
fun InvokeVirtual(function: HairFunction): InvokeVirtual.Form = InvokeVirtual.Form(nodeBuilder.session.invokeVirtualMetaForm, function).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun InvokeVirtual.Form.invoke(control: Controlling?, vararg callArgs: Node?): Controlling = (nodeBuilder.normalize(InvokeVirtual(this@invoke, control, *callArgs)) as Controlling).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun InvokeVirtual.Form.invoke(vararg callArgs: Node?): Controlling = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, *callArgs) }

