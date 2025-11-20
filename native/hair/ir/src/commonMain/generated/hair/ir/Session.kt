package hair.ir

import hair.ir.nodes.*
import hair.sym.*

class Session: SessionBase() {
    // Simple forms

    internal val noValueForm = NoValue.form(this).also { register(it) }

    internal val unitValueForm = UnitValue.form(this).also { register(it) }

    internal val useForm = Use.form(this).also { register(it) }

    internal val unreachableForm = Unreachable.form(this).also { register(it) }

    internal val blockEntryForm = BlockEntry.form(this).also { register(it) }

    internal val returnForm = Return.form(this).also { register(it) }

    internal val gotoForm = Goto.form(this).also { register(it) }

    internal val ifForm = If.form(this).also { register(it) }

    internal val trueExitForm = TrueExit.form(this).also { register(it) }

    internal val falseExitForm = FalseExit.form(this).also { register(it) }

    internal val throwForm = Throw.form(this).also { register(it) }

    internal val unwindForm = Unwind.form(this).also { register(it) }

    internal val catchForm = Catch.form(this).also { register(it) }

    internal val trueForm = True.form(this).also { register(it) }

    internal val falseForm = False.form(this).also { register(it) }

    internal val nullForm = Null.form(this).also { register(it) }

    

    // Meta forms

    internal val readVarMetaForm = ReadVar.metaForm(this)

    internal val assignVarMetaForm = AssignVar.metaForm(this)

    internal val phiMetaForm = Phi.metaForm(this)

    internal val phiPlaceholderMetaForm = PhiPlaceholder.metaForm(this)

    internal val paramMetaForm = Param.metaForm(this)

    internal val constIMetaForm = ConstI.metaForm(this)

    internal val constLMetaForm = ConstL.metaForm(this)

    internal val constFMetaForm = ConstF.metaForm(this)

    internal val constDMetaForm = ConstD.metaForm(this)

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

    internal val cmpMetaForm = Cmp.metaForm(this)

    internal val newMetaForm = New.metaForm(this)

    internal val readFieldPinnedMetaForm = ReadFieldPinned.metaForm(this)

    internal val readGlobalPinnedMetaForm = ReadGlobalPinned.metaForm(this)

    internal val writeFieldMetaForm = WriteField.metaForm(this)

    internal val writeGlobalMetaForm = WriteGlobal.metaForm(this)

    internal val isInstanceOfMetaForm = IsInstanceOf.metaForm(this)

    internal val checkCastMetaForm = CheckCast.metaForm(this)

    internal val invokeStaticMetaForm = InvokeStatic.metaForm(this)

    internal val invokeVirtualMetaForm = InvokeVirtual.metaForm(this)

    

    val entry by lazy { BlockEntry(blockEntryForm).register() }

    val unreachable by lazy { Unreachable(unreachableForm).register() }

}


