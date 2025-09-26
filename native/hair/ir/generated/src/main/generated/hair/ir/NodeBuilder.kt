@file:Suppress("FunctionName")

package hair.ir

import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.Primitive

interface NodeBuilder {
    val session: Session
    
    fun Placeholder(tag: Any): Placeholder.Form
    fun ReadVarForm(variable: Var): ReadVar.Form
    fun AssignVar(variable: Var): AssignVar.Form
    fun ParamForm(number: Int): Param.Form
    fun ConstIntForm(value: Long): ConstInt.Form
    fun ConstFloatForm(value: Double): ConstFloat.Form
    fun Add(type: Primitive): Add.Form
    fun Sub(type: Primitive): Sub.Form
    fun Mul(type: Primitive): Mul.Form
    fun Div(type: Primitive): Div.Form
    fun Rem(type: Primitive): Rem.Form
    fun And(type: Primitive): And.Form
    fun Or(type: Primitive): Or.Form
    fun Xor(type: Primitive): Xor.Form
    fun Shl(type: Primitive): Shl.Form
    fun Shr(type: Primitive): Shr.Form
    fun Ushr(type: Primitive): Ushr.Form
    fun New(type: Class): New.Form
    fun ReadFieldPinned(field: Field): ReadFieldPinned.Form
    fun ReadGlobalPinned(field: Global): ReadGlobalPinned.Form
    fun WriteField(field: Field): WriteField.Form
    fun WriteGlobal(field: Global): WriteGlobal.Form
    fun ReadFieldFloating(field: Field): ReadFieldFloating.Form
    fun ReadGlobalFloating(field: Global): ReadGlobalFloating.Form
    fun IsInstance(type: Class): IsInstance.Form
    fun Cast(type: Class): Cast.Form
    fun StaticCall(function: HairFunction): StaticCall.Form
    
    fun NoValue(): NoValue
    operator fun Placeholder.Form.invoke(vararg inputs: Node): Node
    fun Use(value: Node): Use
    fun Goto(): Goto
    fun If(condition: Node): If
    fun Halt(): Halt
    fun BBlock(): BBlock
    fun XBlock(): XBlock
    fun Throw(exception: Node): Throw
    fun Catch(xBlock: XBlock, vararg catchedValues: Node): Node
    operator fun ReadVar.Form.invoke(): ReadVar
    fun ReadVar(variable: Var): ReadVar
    operator fun AssignVar.Form.invoke(assignedValue: Node): AssignVar
    fun Phi(block: Block, vararg joinedValues: Node): Node
    operator fun Param.Form.invoke(): Param
    fun Param(number: Int): Param
    operator fun ConstInt.Form.invoke(): ConstInt
    fun ConstInt(value: Long): ConstInt
    operator fun ConstFloat.Form.invoke(): ConstFloat
    fun ConstFloat(value: Double): ConstFloat
    operator fun Add.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Sub.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Mul.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Div.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Rem.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun And.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Or.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Xor.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Shl.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Shr.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun Ushr.Form.invoke(lhs: Node, rhs: Node): Node
    operator fun New.Form.invoke(lastLocationAccess: Node): New
    operator fun ReadFieldPinned.Form.invoke(lastLocationAccess: Node, obj: Node): ReadFieldPinned
    operator fun ReadGlobalPinned.Form.invoke(lastLocationAccess: Node): ReadGlobalPinned
    operator fun WriteField.Form.invoke(lastLocationAccess: Node, obj: Node, value: Node): WriteField
    operator fun WriteGlobal.Form.invoke(lastLocationAccess: Node, value: Node): WriteGlobal
    operator fun ReadFieldFloating.Form.invoke(lastLocationAccess: Node, obj: Node): Node
    operator fun ReadGlobalFloating.Form.invoke(lastLocationAccess: Node): Node
    operator fun IsInstance.Form.invoke(obj: Node): IsInstance
    operator fun Cast.Form.invoke(obj: Node): Cast
    fun IndistinctMemory(vararg inputs: Node): Node
    fun Unknown(): Unknown
    fun Escape(owner: ControlFlow, origin: Node, into: Node): Node
    fun Overwrite(owner: ControlFlow, origin: Node): Node
    fun NeqFilter(owner: ControlFlow, origin: Node, to: Node): Node
    operator fun StaticCall.Form.invoke(lastLocationAccess: Node, vararg callArgs: Node): StaticCall
    fun Return(lastLocationAccess: Node, value: Node): Return
}

 class BaseNodeBuilderImpl(override val session: Session): NodeBuilder {
    override fun Placeholder(tag: Any): Placeholder.Form = Placeholder.Form(session.placeholderMetaForm, tag).ensureFormUniq()
    override fun ReadVarForm(variable: Var): ReadVar.Form = ReadVar.Form(session.readVarMetaForm, variable).ensureFormUniq()
    override fun AssignVar(variable: Var): AssignVar.Form = AssignVar.Form(session.assignVarMetaForm, variable).ensureFormUniq()
    override fun ParamForm(number: Int): Param.Form = Param.Form(session.paramMetaForm, number).ensureFormUniq()
    override fun ConstIntForm(value: Long): ConstInt.Form = ConstInt.Form(session.constIntMetaForm, value).ensureFormUniq()
    override fun ConstFloatForm(value: Double): ConstFloat.Form = ConstFloat.Form(session.constFloatMetaForm, value).ensureFormUniq()
    override fun Add(type: Primitive): Add.Form = Add.Form(session.addMetaForm, type).ensureFormUniq()
    override fun Sub(type: Primitive): Sub.Form = Sub.Form(session.subMetaForm, type).ensureFormUniq()
    override fun Mul(type: Primitive): Mul.Form = Mul.Form(session.mulMetaForm, type).ensureFormUniq()
    override fun Div(type: Primitive): Div.Form = Div.Form(session.divMetaForm, type).ensureFormUniq()
    override fun Rem(type: Primitive): Rem.Form = Rem.Form(session.remMetaForm, type).ensureFormUniq()
    override fun And(type: Primitive): And.Form = And.Form(session.andMetaForm, type).ensureFormUniq()
    override fun Or(type: Primitive): Or.Form = Or.Form(session.orMetaForm, type).ensureFormUniq()
    override fun Xor(type: Primitive): Xor.Form = Xor.Form(session.xorMetaForm, type).ensureFormUniq()
    override fun Shl(type: Primitive): Shl.Form = Shl.Form(session.shlMetaForm, type).ensureFormUniq()
    override fun Shr(type: Primitive): Shr.Form = Shr.Form(session.shrMetaForm, type).ensureFormUniq()
    override fun Ushr(type: Primitive): Ushr.Form = Ushr.Form(session.ushrMetaForm, type).ensureFormUniq()
    override fun New(type: Class): New.Form = New.Form(session.newMetaForm, type).ensureFormUniq()
    override fun ReadFieldPinned(field: Field): ReadFieldPinned.Form = ReadFieldPinned.Form(session.readFieldPinnedMetaForm, field).ensureFormUniq()
    override fun ReadGlobalPinned(field: Global): ReadGlobalPinned.Form = ReadGlobalPinned.Form(session.readGlobalPinnedMetaForm, field).ensureFormUniq()
    override fun WriteField(field: Field): WriteField.Form = WriteField.Form(session.writeFieldMetaForm, field).ensureFormUniq()
    override fun WriteGlobal(field: Global): WriteGlobal.Form = WriteGlobal.Form(session.writeGlobalMetaForm, field).ensureFormUniq()
    override fun ReadFieldFloating(field: Field): ReadFieldFloating.Form = ReadFieldFloating.Form(session.readFieldFloatingMetaForm, field).ensureFormUniq()
    override fun ReadGlobalFloating(field: Global): ReadGlobalFloating.Form = ReadGlobalFloating.Form(session.readGlobalFloatingMetaForm, field).ensureFormUniq()
    override fun IsInstance(type: Class): IsInstance.Form = IsInstance.Form(session.isInstanceMetaForm, type).ensureFormUniq()
    override fun Cast(type: Class): Cast.Form = Cast.Form(session.castMetaForm, type).ensureFormUniq()
    override fun StaticCall(function: HairFunction): StaticCall.Form = StaticCall.Form(session.staticCallMetaForm, function).ensureFormUniq()
    
    override fun NoValue(): NoValue = NoValue(session.noValueForm)
    override operator fun Placeholder.Form.invoke(vararg inputs: Node): Placeholder = Placeholder(this@invoke, *inputs)
    override fun Use(value: Node): Use = Use(session.useForm, value)
    override fun Goto(): Goto = Goto(session.gotoForm)
    override fun If(condition: Node): If = If(session.ifForm, condition)
    override fun Halt(): Halt = Halt(session.haltForm)
    override fun BBlock(): BBlock = BBlock(session.bBlockForm)
    override fun XBlock(): XBlock = XBlock(session.xBlockForm)
    override fun Throw(exception: Node): Throw = Throw(session.throwForm, exception)
    override fun Catch(xBlock: XBlock, vararg catchedValues: Node): Catch = Catch(session.catchForm, xBlock, *catchedValues)
    override operator fun ReadVar.Form.invoke(): ReadVar = ReadVar(this@invoke)
    override fun ReadVar(variable: Var): ReadVar = ReadVarForm(variable)()
    override operator fun AssignVar.Form.invoke(assignedValue: Node): AssignVar = AssignVar(this@invoke, assignedValue)
    override fun Phi(block: Block, vararg joinedValues: Node): Phi = Phi(session.phiForm, block, *joinedValues)
    override operator fun Param.Form.invoke(): Param = Param(this@invoke)
    override fun Param(number: Int): Param = ParamForm(number)()
    override operator fun ConstInt.Form.invoke(): ConstInt = ConstInt(this@invoke)
    override fun ConstInt(value: Long): ConstInt = ConstIntForm(value)()
    override operator fun ConstFloat.Form.invoke(): ConstFloat = ConstFloat(this@invoke)
    override fun ConstFloat(value: Double): ConstFloat = ConstFloatForm(value)()
    override operator fun Add.Form.invoke(lhs: Node, rhs: Node): Add = Add(this@invoke, lhs, rhs)
    override operator fun Sub.Form.invoke(lhs: Node, rhs: Node): Sub = Sub(this@invoke, lhs, rhs)
    override operator fun Mul.Form.invoke(lhs: Node, rhs: Node): Mul = Mul(this@invoke, lhs, rhs)
    override operator fun Div.Form.invoke(lhs: Node, rhs: Node): Div = Div(this@invoke, lhs, rhs)
    override operator fun Rem.Form.invoke(lhs: Node, rhs: Node): Rem = Rem(this@invoke, lhs, rhs)
    override operator fun And.Form.invoke(lhs: Node, rhs: Node): And = And(this@invoke, lhs, rhs)
    override operator fun Or.Form.invoke(lhs: Node, rhs: Node): Or = Or(this@invoke, lhs, rhs)
    override operator fun Xor.Form.invoke(lhs: Node, rhs: Node): Xor = Xor(this@invoke, lhs, rhs)
    override operator fun Shl.Form.invoke(lhs: Node, rhs: Node): Shl = Shl(this@invoke, lhs, rhs)
    override operator fun Shr.Form.invoke(lhs: Node, rhs: Node): Shr = Shr(this@invoke, lhs, rhs)
    override operator fun Ushr.Form.invoke(lhs: Node, rhs: Node): Ushr = Ushr(this@invoke, lhs, rhs)
    override operator fun New.Form.invoke(lastLocationAccess: Node): New = New(this@invoke, lastLocationAccess)
    override operator fun ReadFieldPinned.Form.invoke(lastLocationAccess: Node, obj: Node): ReadFieldPinned = ReadFieldPinned(this@invoke, lastLocationAccess, obj)
    override operator fun ReadGlobalPinned.Form.invoke(lastLocationAccess: Node): ReadGlobalPinned = ReadGlobalPinned(this@invoke, lastLocationAccess)
    override operator fun WriteField.Form.invoke(lastLocationAccess: Node, obj: Node, value: Node): WriteField = WriteField(this@invoke, lastLocationAccess, obj, value)
    override operator fun WriteGlobal.Form.invoke(lastLocationAccess: Node, value: Node): WriteGlobal = WriteGlobal(this@invoke, lastLocationAccess, value)
    override operator fun ReadFieldFloating.Form.invoke(lastLocationAccess: Node, obj: Node): ReadFieldFloating = ReadFieldFloating(this@invoke, lastLocationAccess, obj)
    override operator fun ReadGlobalFloating.Form.invoke(lastLocationAccess: Node): ReadGlobalFloating = ReadGlobalFloating(this@invoke, lastLocationAccess)
    override operator fun IsInstance.Form.invoke(obj: Node): IsInstance = IsInstance(this@invoke, obj)
    override operator fun Cast.Form.invoke(obj: Node): Cast = Cast(this@invoke, obj)
    override fun IndistinctMemory(vararg inputs: Node): IndistinctMemory = IndistinctMemory(session.indistinctMemoryForm, *inputs)
    override fun Unknown(): Unknown = Unknown(session.unknownForm)
    override fun Escape(owner: ControlFlow, origin: Node, into: Node): Escape = Escape(session.escapeForm, owner, origin, into)
    override fun Overwrite(owner: ControlFlow, origin: Node): Overwrite = Overwrite(session.overwriteForm, owner, origin)
    override fun NeqFilter(owner: ControlFlow, origin: Node, to: Node): NeqFilter = NeqFilter(session.neqFilterForm, owner, origin, to)
    override operator fun StaticCall.Form.invoke(lastLocationAccess: Node, vararg callArgs: Node): StaticCall = StaticCall(this@invoke, lastLocationAccess, *callArgs)
    override fun Return(lastLocationAccess: Node, value: Node): Return = Return(session.returnForm, lastLocationAccess, value)
}


 class NormalizingNodeBuilder(val normalization: Normalization, val baseBuilder: NodeBuilder): NodeBuilder by baseBuilder {
    private fun Node.normalizeAndRegister(): Node = normalization.normalize(this).also { if (!it.registered) it.register() }
    
    override fun NoValue(): NoValue = baseBuilder.NoValue().register()
    override operator fun Placeholder.Form.invoke(vararg inputs: Node): Node = with (baseBuilder) { this@invoke.invoke(*inputs).normalizeAndRegister() }
    override fun Use(value: Node): Use = baseBuilder.Use(value).register()
    override fun Goto(): Goto = baseBuilder.Goto().register()
    override fun If(condition: Node): If = baseBuilder.If(condition).register()
    override fun Halt(): Halt = baseBuilder.Halt().register()
    override fun BBlock(): BBlock = baseBuilder.BBlock().register()
    override fun XBlock(): XBlock = baseBuilder.XBlock().register()
    override fun Throw(exception: Node): Throw = baseBuilder.Throw(exception).register()
    override fun Catch(xBlock: XBlock, vararg catchedValues: Node): Node = baseBuilder.Catch(xBlock, *catchedValues).normalizeAndRegister()
    override operator fun ReadVar.Form.invoke(): ReadVar = with (baseBuilder) { this@invoke.invoke().register() }
    override fun ReadVar(variable: Var): ReadVar = baseBuilder.ReadVar(variable).register()
    override operator fun AssignVar.Form.invoke(assignedValue: Node): AssignVar = with (baseBuilder) { this@invoke.invoke(assignedValue).register() }
    override fun Phi(block: Block, vararg joinedValues: Node): Node = baseBuilder.Phi(block, *joinedValues).normalizeAndRegister()
    override operator fun Param.Form.invoke(): Param = with (baseBuilder) { this@invoke.invoke().register() }
    override fun Param(number: Int): Param = baseBuilder.Param(number).register()
    override operator fun ConstInt.Form.invoke(): ConstInt = with (baseBuilder) { this@invoke.invoke().register() }
    override fun ConstInt(value: Long): ConstInt = baseBuilder.ConstInt(value).register()
    override operator fun ConstFloat.Form.invoke(): ConstFloat = with (baseBuilder) { this@invoke.invoke().register() }
    override fun ConstFloat(value: Double): ConstFloat = baseBuilder.ConstFloat(value).register()
    override operator fun Add.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Sub.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Mul.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Div.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Rem.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun And.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Or.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Xor.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Shl.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Shr.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun Ushr.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).normalizeAndRegister() }
    override operator fun New.Form.invoke(lastLocationAccess: Node): New = with (baseBuilder) { this@invoke.invoke(lastLocationAccess).register() }
    override operator fun ReadFieldPinned.Form.invoke(lastLocationAccess: Node, obj: Node): ReadFieldPinned = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, obj).register() }
    override operator fun ReadGlobalPinned.Form.invoke(lastLocationAccess: Node): ReadGlobalPinned = with (baseBuilder) { this@invoke.invoke(lastLocationAccess).register() }
    override operator fun WriteField.Form.invoke(lastLocationAccess: Node, obj: Node, value: Node): WriteField = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, obj, value).register() }
    override operator fun WriteGlobal.Form.invoke(lastLocationAccess: Node, value: Node): WriteGlobal = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, value).register() }
    override operator fun ReadFieldFloating.Form.invoke(lastLocationAccess: Node, obj: Node): Node = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, obj).normalizeAndRegister() }
    override operator fun ReadGlobalFloating.Form.invoke(lastLocationAccess: Node): Node = with (baseBuilder) { this@invoke.invoke(lastLocationAccess).normalizeAndRegister() }
    override operator fun IsInstance.Form.invoke(obj: Node): IsInstance = with (baseBuilder) { this@invoke.invoke(obj).register() }
    override operator fun Cast.Form.invoke(obj: Node): Cast = with (baseBuilder) { this@invoke.invoke(obj).register() }
    override fun IndistinctMemory(vararg inputs: Node): Node = baseBuilder.IndistinctMemory(*inputs).normalizeAndRegister()
    override fun Unknown(): Unknown = baseBuilder.Unknown().register()
    override fun Escape(owner: ControlFlow, origin: Node, into: Node): Node = baseBuilder.Escape(owner, origin, into).normalizeAndRegister()
    override fun Overwrite(owner: ControlFlow, origin: Node): Node = baseBuilder.Overwrite(owner, origin).normalizeAndRegister()
    override fun NeqFilter(owner: ControlFlow, origin: Node, to: Node): Node = baseBuilder.NeqFilter(owner, origin, to).normalizeAndRegister()
    override operator fun StaticCall.Form.invoke(lastLocationAccess: Node, vararg callArgs: Node): StaticCall = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, *callArgs).register() }
    override fun Return(lastLocationAccess: Node, value: Node): Return = baseBuilder.Return(lastLocationAccess, value).register()
}


abstract class ObservingNodeBuilder(val baseBuilder: NodeBuilder): NodeBuilder by baseBuilder {
    abstract fun onNodeBuilt(node: Node)
    
    override fun NoValue(): NoValue = baseBuilder.NoValue().also { onNodeBuilt(it) }
    override operator fun Placeholder.Form.invoke(vararg inputs: Node): Node = with (baseBuilder) { this@invoke.invoke(*inputs).also { onNodeBuilt(it) } }
    override fun Use(value: Node): Use = baseBuilder.Use(value).also { onNodeBuilt(it) }
    override fun Goto(): Goto = baseBuilder.Goto().also { onNodeBuilt(it) }
    override fun If(condition: Node): If = baseBuilder.If(condition).also { onNodeBuilt(it) }
    override fun Halt(): Halt = baseBuilder.Halt().also { onNodeBuilt(it) }
    override fun BBlock(): BBlock = baseBuilder.BBlock().also { onNodeBuilt(it) }
    override fun XBlock(): XBlock = baseBuilder.XBlock().also { onNodeBuilt(it) }
    override fun Throw(exception: Node): Throw = baseBuilder.Throw(exception).also { onNodeBuilt(it) }
    override fun Catch(xBlock: XBlock, vararg catchedValues: Node): Node = baseBuilder.Catch(xBlock, *catchedValues).also { onNodeBuilt(it) }
    override operator fun ReadVar.Form.invoke(): ReadVar = with (baseBuilder) { this@invoke.invoke().also { onNodeBuilt(it) } }
    override fun ReadVar(variable: Var): ReadVar = baseBuilder.ReadVar(variable).also { onNodeBuilt(it) }
    override operator fun AssignVar.Form.invoke(assignedValue: Node): AssignVar = with (baseBuilder) { this@invoke.invoke(assignedValue).also { onNodeBuilt(it) } }
    override fun Phi(block: Block, vararg joinedValues: Node): Node = baseBuilder.Phi(block, *joinedValues).also { onNodeBuilt(it) }
    override operator fun Param.Form.invoke(): Param = with (baseBuilder) { this@invoke.invoke().also { onNodeBuilt(it) } }
    override fun Param(number: Int): Param = baseBuilder.Param(number).also { onNodeBuilt(it) }
    override operator fun ConstInt.Form.invoke(): ConstInt = with (baseBuilder) { this@invoke.invoke().also { onNodeBuilt(it) } }
    override fun ConstInt(value: Long): ConstInt = baseBuilder.ConstInt(value).also { onNodeBuilt(it) }
    override operator fun ConstFloat.Form.invoke(): ConstFloat = with (baseBuilder) { this@invoke.invoke().also { onNodeBuilt(it) } }
    override fun ConstFloat(value: Double): ConstFloat = baseBuilder.ConstFloat(value).also { onNodeBuilt(it) }
    override operator fun Add.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Sub.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Mul.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Div.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Rem.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun And.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Or.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Xor.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Shl.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Shr.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun Ushr.Form.invoke(lhs: Node, rhs: Node): Node = with (baseBuilder) { this@invoke.invoke(lhs, rhs).also { onNodeBuilt(it) } }
    override operator fun New.Form.invoke(lastLocationAccess: Node): New = with (baseBuilder) { this@invoke.invoke(lastLocationAccess).also { onNodeBuilt(it) } }
    override operator fun ReadFieldPinned.Form.invoke(lastLocationAccess: Node, obj: Node): ReadFieldPinned = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, obj).also { onNodeBuilt(it) } }
    override operator fun ReadGlobalPinned.Form.invoke(lastLocationAccess: Node): ReadGlobalPinned = with (baseBuilder) { this@invoke.invoke(lastLocationAccess).also { onNodeBuilt(it) } }
    override operator fun WriteField.Form.invoke(lastLocationAccess: Node, obj: Node, value: Node): WriteField = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, obj, value).also { onNodeBuilt(it) } }
    override operator fun WriteGlobal.Form.invoke(lastLocationAccess: Node, value: Node): WriteGlobal = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, value).also { onNodeBuilt(it) } }
    override operator fun ReadFieldFloating.Form.invoke(lastLocationAccess: Node, obj: Node): Node = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, obj).also { onNodeBuilt(it) } }
    override operator fun ReadGlobalFloating.Form.invoke(lastLocationAccess: Node): Node = with (baseBuilder) { this@invoke.invoke(lastLocationAccess).also { onNodeBuilt(it) } }
    override operator fun IsInstance.Form.invoke(obj: Node): IsInstance = with (baseBuilder) { this@invoke.invoke(obj).also { onNodeBuilt(it) } }
    override operator fun Cast.Form.invoke(obj: Node): Cast = with (baseBuilder) { this@invoke.invoke(obj).also { onNodeBuilt(it) } }
    override fun IndistinctMemory(vararg inputs: Node): Node = baseBuilder.IndistinctMemory(*inputs).also { onNodeBuilt(it) }
    override fun Unknown(): Unknown = baseBuilder.Unknown().also { onNodeBuilt(it) }
    override fun Escape(owner: ControlFlow, origin: Node, into: Node): Node = baseBuilder.Escape(owner, origin, into).also { onNodeBuilt(it) }
    override fun Overwrite(owner: ControlFlow, origin: Node): Node = baseBuilder.Overwrite(owner, origin).also { onNodeBuilt(it) }
    override fun NeqFilter(owner: ControlFlow, origin: Node, to: Node): Node = baseBuilder.NeqFilter(owner, origin, to).also { onNodeBuilt(it) }
    override operator fun StaticCall.Form.invoke(lastLocationAccess: Node, vararg callArgs: Node): StaticCall = with (baseBuilder) { this@invoke.invoke(lastLocationAccess, *callArgs).also { onNodeBuilt(it) } }
    override fun Return(lastLocationAccess: Node, value: Node): Return = baseBuilder.Return(lastLocationAccess, value).also { onNodeBuilt(it) }
}


