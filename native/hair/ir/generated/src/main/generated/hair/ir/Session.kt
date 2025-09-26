package hair.ir

import hair.ir.nodes.*
import hair.sym.*

class Session: SessionBase(), ArgsAccessor {
    // Simple forms
    internal val noValueForm = NoValue.form(this).also { register(it) }
    internal val useForm = Use.form(this).also { register(it) }
    internal val gotoForm = Goto.form(this).also { register(it) }
    internal val ifForm = If.form(this).also { register(it) }
    internal val haltForm = Halt.form(this).also { register(it) }
    internal val bBlockForm = BBlock.form(this).also { register(it) }
    internal val xBlockForm = XBlock.form(this).also { register(it) }
    internal val throwForm = Throw.form(this).also { register(it) }
    internal val catchForm = Catch.form(this).also { register(it) }
    internal val phiForm = Phi.form(this).also { register(it) }
    internal val indistinctMemoryForm = IndistinctMemory.form(this).also { register(it) }
    internal val unknownForm = Unknown.form(this).also { register(it) }
    internal val escapeForm = Escape.form(this).also { register(it) }
    internal val overwriteForm = Overwrite.form(this).also { register(it) }
    internal val neqFilterForm = NeqFilter.form(this).also { register(it) }
    internal val returnForm = Return.form(this).also { register(it) }
    
    // Meta forms
    internal val placeholderMetaForm = Placeholder.metaForm(this)
    internal val readVarMetaForm = ReadVar.metaForm(this)
    internal val assignVarMetaForm = AssignVar.metaForm(this)
    internal val paramMetaForm = Param.metaForm(this)
    internal val constIntMetaForm = ConstInt.metaForm(this)
    internal val constFloatMetaForm = ConstFloat.metaForm(this)
    internal val addMetaForm = Add.metaForm(this)
    internal val subMetaForm = Sub.metaForm(this)
    internal val mulMetaForm = Mul.metaForm(this)
    internal val divMetaForm = Div.metaForm(this)
    internal val remMetaForm = Rem.metaForm(this)
    internal val andMetaForm = And.metaForm(this)
    internal val orMetaForm = Or.metaForm(this)
    internal val xorMetaForm = Xor.metaForm(this)
    internal val shlMetaForm = Shl.metaForm(this)
    internal val shrMetaForm = Shr.metaForm(this)
    internal val ushrMetaForm = Ushr.metaForm(this)
    internal val newMetaForm = New.metaForm(this)
    internal val readFieldPinnedMetaForm = ReadFieldPinned.metaForm(this)
    internal val readGlobalPinnedMetaForm = ReadGlobalPinned.metaForm(this)
    internal val writeFieldMetaForm = WriteField.metaForm(this)
    internal val writeGlobalMetaForm = WriteGlobal.metaForm(this)
    internal val readFieldFloatingMetaForm = ReadFieldFloating.metaForm(this)
    internal val readGlobalFloatingMetaForm = ReadGlobalFloating.metaForm(this)
    internal val isInstanceMetaForm = IsInstance.metaForm(this)
    internal val castMetaForm = Cast.metaForm(this)
    internal val staticCallMetaForm = StaticCall.metaForm(this)
    
    val entryBlock by lazy { BBlock(blockForm).register() }
}

