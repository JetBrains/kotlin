package hair.ir

import hair.ir.nodes.*
import hair.sym.*

class Session: SessionBase() {
    // Simple forms

    internal val noValueForm = NoValue.form(this).also { register(it) }

    internal val useForm = Use.form(this).also { register(it) }

    internal val blockEntryForm = BlockEntry.form(this).also { register(it) }

    internal val returnForm = Return.form(this).also { register(it) }

    internal val gotoForm = Goto.form(this).also { register(it) }

    internal val ifForm = If.form(this).also { register(it) }

    internal val ifTrueForm = If.True.form(this).also { register(it) }

    internal val ifFalseForm = If.False.form(this).also { register(it) }

    internal val throwForm = Throw.form(this).also { register(it) }

    internal val unwindForm = Unwind.form(this).also { register(it) }

    internal val phiForm = Phi.form(this).also { register(it) }

    internal val catchForm = Catch.form(this).also { register(it) }

    internal val addIForm = AddI.form(this).also { register(it) }

    internal val subIForm = SubI.form(this).also { register(it) }

    internal val mulIForm = MulI.form(this).also { register(it) }

    internal val divIForm = DivI.form(this).also { register(it) }

    internal val remIForm = RemI.form(this).also { register(it) }

    internal val addLForm = AddL.form(this).also { register(it) }

    internal val subLForm = SubL.form(this).also { register(it) }

    internal val mulLForm = MulL.form(this).also { register(it) }

    internal val divLForm = DivL.form(this).also { register(it) }

    internal val remLForm = RemL.form(this).also { register(it) }

    internal val addFForm = AddF.form(this).also { register(it) }

    internal val subFForm = SubF.form(this).also { register(it) }

    internal val mulFForm = MulF.form(this).also { register(it) }

    internal val divFForm = DivF.form(this).also { register(it) }

    internal val remFForm = RemF.form(this).also { register(it) }

    internal val addDForm = AddD.form(this).also { register(it) }

    internal val subDForm = SubD.form(this).also { register(it) }

    internal val mulDForm = MulD.form(this).also { register(it) }

    internal val divDForm = DivD.form(this).also { register(it) }

    internal val remDForm = RemD.form(this).also { register(it) }

    

    // Meta forms

    internal val readVarMetaForm = ReadVar.metaForm(this)

    internal val assignVarMetaForm = AssignVar.metaForm(this)

    internal val paramMetaForm = Param.metaForm(this)

    internal val constIMetaForm = ConstI.metaForm(this)

    internal val constLMetaForm = ConstL.metaForm(this)

    internal val constFMetaForm = ConstF.metaForm(this)

    internal val constDMetaForm = ConstD.metaForm(this)

    internal val newMetaForm = New.metaForm(this)

    internal val readFieldPinnedMetaForm = ReadFieldPinned.metaForm(this)

    internal val readGlobalPinnedMetaForm = ReadGlobalPinned.metaForm(this)

    internal val writeFieldMetaForm = WriteField.metaForm(this)

    internal val writeGlobalMetaForm = WriteGlobal.metaForm(this)

    internal val isInstanceOfMetaForm = IsInstanceOf.metaForm(this)

    internal val castMetaForm = Cast.metaForm(this)

    internal val staticCallMetaForm = StaticCall.metaForm(this)

    

    val entry by lazy { BlockEntry(blockEntryForm).register() }

}


