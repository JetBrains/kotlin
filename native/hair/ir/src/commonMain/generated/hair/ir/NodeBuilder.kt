@file:Suppress("FunctionName")

package hair.ir
import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.*

context(nodeBuilder: NodeBuilder)
fun NoValue(): NoValue = nodeBuilder.register(NoValue(nodeBuilder.session.noValueForm))

context(nodeBuilder: NodeBuilder)
fun Use(control: Controlling?, value: Node?): Use = nodeBuilder.register(Use(nodeBuilder.session.useForm, control, value))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Use(value: Node?): Use = controlBuilder.appendControlled { ctrl -> Use(ctrl, value) }

context(nodeBuilder: NodeBuilder)
fun BlockEntryNoCtrl(vararg preds: BlockExit?): BlockEntry = nodeBuilder.register(BlockEntry(nodeBuilder.session.blockEntryForm, *preds))

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun BlockEntry(vararg preds: BlockExit?): BlockEntry = BlockEntryNoCtrl(*preds)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun BlockEntry(vararg preds: BlockExit?): BlockEntry = controlBuilder.appendControl { BlockEntryNoCtrl(*preds) }

context(nodeBuilder: NodeBuilder)
fun Return(control: Controlling?, result: Node?): Return = nodeBuilder.register(Return(nodeBuilder.session.returnForm, control, result))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Return(result: Node?): Return = controlBuilder.appendControlled { ctrl -> Return(ctrl, result) }

context(nodeBuilder: NodeBuilder)
fun Goto(control: Controlling?): Goto = nodeBuilder.register(Goto(nodeBuilder.session.gotoForm, control))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Goto(): Goto = controlBuilder.appendControlled { ctrl -> Goto(ctrl) }

context(nodeBuilder: NodeBuilder)
fun IfTrue(owner: If?): If.True = nodeBuilder.register(If.True(nodeBuilder.session.ifTrueForm, owner))

context(nodeBuilder: NodeBuilder)
fun IfFalse(owner: If?): If.False = nodeBuilder.register(If.False(nodeBuilder.session.ifFalseForm, owner))

context(nodeBuilder: NodeBuilder)
fun If(control: Controlling?, cond: Node?): If = nodeBuilder.register(If(nodeBuilder.session.ifForm, control, cond))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun If(cond: Node?): If = controlBuilder.appendControlled { ctrl -> If(ctrl, cond) }

context(nodeBuilder: NodeBuilder)
fun Throw(control: Controlling?, exception: Node?): Throw = nodeBuilder.register(Throw(nodeBuilder.session.throwForm, control, exception))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun Throw(exception: Node?): Throw = controlBuilder.appendControlled { ctrl -> Throw(ctrl, exception) }

context(nodeBuilder: NodeBuilder)
fun Unwind(thrower: Throwing?): Unwind = nodeBuilder.register(Unwind(nodeBuilder.session.unwindForm, thrower))

context(nodeBuilder: NodeBuilder)
private fun ReadVarForm(variable: Any): ReadVar.Form = ReadVar.Form(nodeBuilder.session.readVarMetaForm, variable).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun ReadVar(variable: Any): ReadVar.Form = ReadVarForm(variable)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ReadVar(variable: Any): ReadVar = ReadVarForm(variable)()

context(nodeBuilder: NodeBuilder)
operator fun ReadVar.Form.invoke(control: Controlling?): ReadVar = nodeBuilder.register(ReadVar(this@invoke, control))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun ReadVar.Form.invoke(): ReadVar = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun AssignVar(variable: Any): AssignVar.Form = AssignVar.Form(nodeBuilder.session.assignVarMetaForm, variable).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun AssignVar.Form.invoke(control: Controlling?, assignedValue: Node?): AssignVar = nodeBuilder.register(AssignVar(this@invoke, control, assignedValue))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun AssignVar.Form.invoke(assignedValue: Node?): AssignVar = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, assignedValue) }

context(nodeBuilder: NodeBuilder)
fun Phi(block: BlockEntry?, vararg joinedValues: Node?): Node = nodeBuilder.normalize(Phi(nodeBuilder.session.phiForm, block, *joinedValues)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ParamForm(index: Int): Param.Form = Param.Form(nodeBuilder.session.paramMetaForm, index).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Param.Form.invoke(): Param = nodeBuilder.register(Param(this@invoke))

context(nodeBuilder: NodeBuilder)
fun Param(index: Int): Param = ParamForm(index)()

context(nodeBuilder: NodeBuilder)
fun Catch(unwind: Node?): Node = nodeBuilder.normalize(Catch(nodeBuilder.session.catchForm, unwind)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ConstIForm(value: Int): ConstI.Form = ConstI.Form(nodeBuilder.session.constIMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstI.Form.invoke(): ConstI = nodeBuilder.register(ConstI(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstI(value: Int): ConstI = ConstIForm(value)()

context(nodeBuilder: NodeBuilder)
fun AddI(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(AddI(nodeBuilder.session.addIForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun SubI(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(SubI(nodeBuilder.session.subIForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun MulI(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(MulI(nodeBuilder.session.mulIForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun DivI(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(DivI(nodeBuilder.session.divIForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun RemI(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(RemI(nodeBuilder.session.remIForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ConstLForm(value: Long): ConstL.Form = ConstL.Form(nodeBuilder.session.constLMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstL.Form.invoke(): ConstL = nodeBuilder.register(ConstL(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstL(value: Long): ConstL = ConstLForm(value)()

context(nodeBuilder: NodeBuilder)
fun AddL(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(AddL(nodeBuilder.session.addLForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun SubL(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(SubL(nodeBuilder.session.subLForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun MulL(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(MulL(nodeBuilder.session.mulLForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun DivL(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(DivL(nodeBuilder.session.divLForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun RemL(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(RemL(nodeBuilder.session.remLForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ConstFForm(value: Float): ConstF.Form = ConstF.Form(nodeBuilder.session.constFMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstF.Form.invoke(): ConstF = nodeBuilder.register(ConstF(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstF(value: Float): ConstF = ConstFForm(value)()

context(nodeBuilder: NodeBuilder)
fun AddF(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(AddF(nodeBuilder.session.addFForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun SubF(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(SubF(nodeBuilder.session.subFForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun MulF(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(MulF(nodeBuilder.session.mulFForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun DivF(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(DivF(nodeBuilder.session.divFForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun RemF(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(RemF(nodeBuilder.session.remFForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun ConstDForm(value: Double): ConstD.Form = ConstD.Form(nodeBuilder.session.constDMetaForm, value).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ConstD.Form.invoke(): ConstD = nodeBuilder.register(ConstD(this@invoke))

context(nodeBuilder: NodeBuilder)
fun ConstD(value: Double): ConstD = ConstDForm(value)()

context(nodeBuilder: NodeBuilder)
fun AddD(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(AddD(nodeBuilder.session.addDForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun SubD(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(SubD(nodeBuilder.session.subDForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun MulD(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(MulD(nodeBuilder.session.mulDForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun DivD(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(DivD(nodeBuilder.session.divDForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun RemD(lhs: Node?, rhs: Node?): Node = nodeBuilder.normalize(RemD(nodeBuilder.session.remDForm, lhs, rhs)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
private fun NewForm(type: Class): New.Form = New.Form(nodeBuilder.session.newMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun New(type: Class): New.Form = NewForm(type)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun New(type: Class): New = NewForm(type)()

context(nodeBuilder: NodeBuilder)
operator fun New.Form.invoke(control: Controlling?): New = nodeBuilder.register(New(this@invoke, control))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun New.Form.invoke(): New = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun ReadFieldPinned(field: Field): ReadFieldPinned.Form = ReadFieldPinned.Form(nodeBuilder.session.readFieldPinnedMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun ReadFieldPinned.Form.invoke(control: Controlling?, obj: Node?): ReadFieldPinned = nodeBuilder.register(ReadFieldPinned(this@invoke, control, obj))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun ReadFieldPinned.Form.invoke(obj: Node?): ReadFieldPinned = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, obj) }

context(nodeBuilder: NodeBuilder)
private fun ReadGlobalPinnedForm(field: Global): ReadGlobalPinned.Form = ReadGlobalPinned.Form(nodeBuilder.session.readGlobalPinnedMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder, _: NoControlFlowBuilder)
fun ReadGlobalPinned(field: Global): ReadGlobalPinned.Form = ReadGlobalPinnedForm(field)

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
fun ReadGlobalPinned(field: Global): ReadGlobalPinned = ReadGlobalPinnedForm(field)()

context(nodeBuilder: NodeBuilder)
operator fun ReadGlobalPinned.Form.invoke(control: Controlling?): ReadGlobalPinned = nodeBuilder.register(ReadGlobalPinned(this@invoke, control))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun ReadGlobalPinned.Form.invoke(): ReadGlobalPinned = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl) }

context(nodeBuilder: NodeBuilder)
fun WriteField(field: Field): WriteField.Form = WriteField.Form(nodeBuilder.session.writeFieldMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun WriteField.Form.invoke(control: Controlling?, obj: Node?, value: Node?): WriteField = nodeBuilder.register(WriteField(this@invoke, control, obj, value))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun WriteField.Form.invoke(obj: Node?, value: Node?): WriteField = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, obj, value) }

context(nodeBuilder: NodeBuilder)
fun WriteGlobal(field: Global): WriteGlobal.Form = WriteGlobal.Form(nodeBuilder.session.writeGlobalMetaForm, field).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun WriteGlobal.Form.invoke(control: Controlling?, value: Node?): WriteGlobal = nodeBuilder.register(WriteGlobal(this@invoke, control, value))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun WriteGlobal.Form.invoke(value: Node?): WriteGlobal = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, value) }

context(nodeBuilder: NodeBuilder)
fun IsInstanceOf(type: Reference): IsInstanceOf.Form = IsInstanceOf.Form(nodeBuilder.session.isInstanceOfMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun IsInstanceOf.Form.invoke(obj: Node?): Node = nodeBuilder.normalize(IsInstanceOf(this@invoke, obj)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun Cast(type: Reference): Cast.Form = Cast.Form(nodeBuilder.session.castMetaForm, type).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun Cast.Form.invoke(obj: Node?): Node = nodeBuilder.normalize(Cast(this@invoke, obj)).let { if (!it.registered) nodeBuilder.register(it) else it }

context(nodeBuilder: NodeBuilder)
fun StaticCall(function: HairFunction): StaticCall.Form = StaticCall.Form(nodeBuilder.session.staticCallMetaForm, function).ensureFormUniq()

context(nodeBuilder: NodeBuilder)
operator fun StaticCall.Form.invoke(control: Controlling?, vararg callArgs: Node?): StaticCall = nodeBuilder.register(StaticCall(this@invoke, control, *callArgs))

context(nodeBuilder: NodeBuilder, controlBuilder: ControlFlowBuilder)
operator fun StaticCall.Form.invoke(vararg callArgs: Node?): StaticCall = controlBuilder.appendControlled { ctrl -> this@invoke(ctrl, *callArgs) }

