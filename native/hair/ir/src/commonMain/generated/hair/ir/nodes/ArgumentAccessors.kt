package hair.ir.nodes

import hair.sym.*
import hair.ir.*
import hair.sym.Type.*

val Projection.owner: ControlFlow
    get() = args[ownerIndex] as ControlFlow
val Projection.ownerOrNull: ControlFlow?
    get() = args.getOrNull(ownerIndex)?.let { it as ControlFlow }

val Cast.operand: Node
    get() = args[operandIndex]
val Cast.operandOrNull: Node?
    get() = args.getOrNull(operandIndex)

val TypeCheck.obj: Node
    get() = args[objIndex]
val TypeCheck.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val AnyStore.value: Node
    get() = args[valueIndex]
val AnyStore.valueOrNull: Node?
    get() = args.getOrNull(valueIndex)

val InstanceFieldOp.obj: Node
    get() = args[objIndex]
val InstanceFieldOp.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val Use.value: Node
    get() = args[valueIndex]
val Use.valueOrNull: Node?
    get() = args.getOrNull(valueIndex)

val BlockEntry.preds: VarArgsList<BlockExit>
    get() = VarArgsList(args, predsIndex, BlockExit::class)

val Controlled.control: Controlling
    get() = args[controlIndex] as Controlling
val Controlled.controlOrNull: Controlling?
    get() = args.getOrNull(controlIndex)?.let { it as Controlling }

val Return.result: Node
    get() = args[resultIndex]
val Return.resultOrNull: Node?
    get() = args.getOrNull(resultIndex)

val If.cond: Node
    get() = args[condIndex]
val If.condOrNull: Node?
    get() = args.getOrNull(condIndex)

val IfProjection.owner: If
    get() = args[ownerIndex] as If
val IfProjection.ownerOrNull: If?
    get() = args.getOrNull(ownerIndex)?.let { it as If }

val TrueExit.owner: If
    get() = args[ownerIndex] as If
val TrueExit.ownerOrNull: If?
    get() = args.getOrNull(ownerIndex)?.let { it as If }

val FalseExit.owner: If
    get() = args[ownerIndex] as If
val FalseExit.ownerOrNull: If?
    get() = args.getOrNull(ownerIndex)?.let { it as If }

val Throw.exception: Node
    get() = args[exceptionIndex]
val Throw.exceptionOrNull: Node?
    get() = args.getOrNull(exceptionIndex)

val Unwind.thrower: Throwing
    get() = args[throwerIndex] as Throwing
val Unwind.throwerOrNull: Throwing?
    get() = args.getOrNull(throwerIndex)?.let { it as Throwing }

val AssignVar.assignedValue: Node
    get() = args[assignedValueIndex]
val AssignVar.assignedValueOrNull: Node?
    get() = args.getOrNull(assignedValueIndex)

val Phi.block: BlockEntry
    get() = args[blockIndex] as BlockEntry
val Phi.blockOrNull: BlockEntry?
    get() = args.getOrNull(blockIndex)?.let { it as BlockEntry }

val Phi.joinedValues: VarArgsList<Node>
    get() = VarArgsList(args, joinedValuesIndex, Node::class)

val PhiPlaceholder.block: BlockEntry
    get() = args[blockIndex] as BlockEntry
val PhiPlaceholder.blockOrNull: BlockEntry?
    get() = args.getOrNull(blockIndex)?.let { it as BlockEntry }

val PhiPlaceholder.joinedValues: VarArgsList<Node>
    get() = VarArgsList(args, joinedValuesIndex, Node::class)

val Catch.unwind: Node
    get() = args[unwindIndex]
val Catch.unwindOrNull: Node?
    get() = args.getOrNull(unwindIndex)

val BinaryOp.lhs: Node
    get() = args[lhsIndex]
val BinaryOp.lhsOrNull: Node?
    get() = args.getOrNull(lhsIndex)

val BinaryOp.rhs: Node
    get() = args[rhsIndex]
val BinaryOp.rhsOrNull: Node?
    get() = args.getOrNull(rhsIndex)

val Neg.operand: Node
    get() = args[operandIndex]
val Neg.operandOrNull: Node?
    get() = args.getOrNull(operandIndex)

val Not.operand: Node
    get() = args[operandIndex]
val Not.operandOrNull: Node?
    get() = args.getOrNull(operandIndex)

val SignExtend.operand: Node
    get() = args[operandIndex]
val SignExtend.operandOrNull: Node?
    get() = args.getOrNull(operandIndex)

val ZeroExtend.operand: Node
    get() = args[operandIndex]
val ZeroExtend.operandOrNull: Node?
    get() = args.getOrNull(operandIndex)

val Truncate.operand: Node
    get() = args[operandIndex]
val Truncate.operandOrNull: Node?
    get() = args.getOrNull(operandIndex)

val Reinterpret.operand: Node
    get() = args[operandIndex]
val Reinterpret.operandOrNull: Node?
    get() = args.getOrNull(operandIndex)

val NewArray.size: Node
    get() = args[sizeIndex]
val NewArray.sizeOrNull: Node?
    get() = args.getOrNull(sizeIndex)

val IsInstanceOf.obj: Node
    get() = args[objIndex]
val IsInstanceOf.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val ThrowingCheck.obj: Node
    get() = args[objIndex]
val ThrowingCheck.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val CheckCast.obj: Node
    get() = args[objIndex]
val CheckCast.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val TypeInfo.obj: Node
    get() = args[objIndex]
val TypeInfo.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val DirectMemoryOp.location: Node
    get() = args[locationIndex]
val DirectMemoryOp.locationOrNull: Node?
    get() = args.getOrNull(locationIndex)

val LoadField.obj: Node
    get() = args[objIndex]
val LoadField.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val StoreField.obj: Node
    get() = args[objIndex]
val StoreField.objOrNull: Node?
    get() = args.getOrNull(objIndex)

val StoreField.value: Node
    get() = args[valueIndex]
val StoreField.valueOrNull: Node?
    get() = args.getOrNull(valueIndex)

val StoreGlobal.value: Node
    get() = args[valueIndex]
val StoreGlobal.valueOrNull: Node?
    get() = args.getOrNull(valueIndex)

val AnyInvoke.callArgs: VarArgsList<Node>
    get() = VarArgsList(args, callArgsIndex, Node::class)


interface ArgumentAccessor {
    val Projection.owner: ControlFlow
        get() = args[ownerIndex] as ControlFlow

    val Projection.ownerOrNull: ControlFlow?
        get() = args.getOrNull(ownerIndex)?.let { it as ControlFlow }

    

    val Cast.operand: Node
        get() = args[operandIndex]

    val Cast.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)

    

    val TypeCheck.obj: Node
        get() = args[objIndex]

    val TypeCheck.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val AnyStore.value: Node
        get() = args[valueIndex]

    val AnyStore.valueOrNull: Node?
        get() = args.getOrNull(valueIndex)

    

    val InstanceFieldOp.obj: Node
        get() = args[objIndex]

    val InstanceFieldOp.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val Use.value: Node
        get() = args[valueIndex]

    val Use.valueOrNull: Node?
        get() = args.getOrNull(valueIndex)

    

    val BlockEntry.preds: VarArgsList<BlockExit>
        get() = VarArgsList(args, predsIndex, BlockExit::class)

    

    val Controlled.control: Controlling
        get() = args[controlIndex] as Controlling

    val Controlled.controlOrNull: Controlling?
        get() = args.getOrNull(controlIndex)?.let { it as Controlling }

    

    val Return.result: Node
        get() = args[resultIndex]

    val Return.resultOrNull: Node?
        get() = args.getOrNull(resultIndex)

    

    val If.cond: Node
        get() = args[condIndex]

    val If.condOrNull: Node?
        get() = args.getOrNull(condIndex)

    

    val IfProjection.owner: If
        get() = args[ownerIndex] as If

    val IfProjection.ownerOrNull: If?
        get() = args.getOrNull(ownerIndex)?.let { it as If }

    

    val TrueExit.owner: If
        get() = args[ownerIndex] as If

    val TrueExit.ownerOrNull: If?
        get() = args.getOrNull(ownerIndex)?.let { it as If }

    

    val FalseExit.owner: If
        get() = args[ownerIndex] as If

    val FalseExit.ownerOrNull: If?
        get() = args.getOrNull(ownerIndex)?.let { it as If }

    

    val Throw.exception: Node
        get() = args[exceptionIndex]

    val Throw.exceptionOrNull: Node?
        get() = args.getOrNull(exceptionIndex)

    

    val Unwind.thrower: Throwing
        get() = args[throwerIndex] as Throwing

    val Unwind.throwerOrNull: Throwing?
        get() = args.getOrNull(throwerIndex)?.let { it as Throwing }

    

    val AssignVar.assignedValue: Node
        get() = args[assignedValueIndex]

    val AssignVar.assignedValueOrNull: Node?
        get() = args.getOrNull(assignedValueIndex)

    

    val Phi.block: BlockEntry
        get() = args[blockIndex] as BlockEntry

    val Phi.blockOrNull: BlockEntry?
        get() = args.getOrNull(blockIndex)?.let { it as BlockEntry }

    

    val Phi.joinedValues: VarArgsList<Node>
        get() = VarArgsList(args, joinedValuesIndex, Node::class)

    

    val PhiPlaceholder.block: BlockEntry
        get() = args[blockIndex] as BlockEntry

    val PhiPlaceholder.blockOrNull: BlockEntry?
        get() = args.getOrNull(blockIndex)?.let { it as BlockEntry }

    

    val PhiPlaceholder.joinedValues: VarArgsList<Node>
        get() = VarArgsList(args, joinedValuesIndex, Node::class)

    

    val Catch.unwind: Node
        get() = args[unwindIndex]

    val Catch.unwindOrNull: Node?
        get() = args.getOrNull(unwindIndex)

    

    val BinaryOp.lhs: Node
        get() = args[lhsIndex]

    val BinaryOp.lhsOrNull: Node?
        get() = args.getOrNull(lhsIndex)

    

    val BinaryOp.rhs: Node
        get() = args[rhsIndex]

    val BinaryOp.rhsOrNull: Node?
        get() = args.getOrNull(rhsIndex)

    

    val Neg.operand: Node
        get() = args[operandIndex]

    val Neg.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)

    

    val Not.operand: Node
        get() = args[operandIndex]

    val Not.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)

    

    val SignExtend.operand: Node
        get() = args[operandIndex]

    val SignExtend.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)

    

    val ZeroExtend.operand: Node
        get() = args[operandIndex]

    val ZeroExtend.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)

    

    val Truncate.operand: Node
        get() = args[operandIndex]

    val Truncate.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)

    

    val Reinterpret.operand: Node
        get() = args[operandIndex]

    val Reinterpret.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)

    

    val NewArray.size: Node
        get() = args[sizeIndex]

    val NewArray.sizeOrNull: Node?
        get() = args.getOrNull(sizeIndex)

    

    val IsInstanceOf.obj: Node
        get() = args[objIndex]

    val IsInstanceOf.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val ThrowingCheck.obj: Node
        get() = args[objIndex]

    val ThrowingCheck.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val CheckCast.obj: Node
        get() = args[objIndex]

    val CheckCast.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val TypeInfo.obj: Node
        get() = args[objIndex]

    val TypeInfo.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val DirectMemoryOp.location: Node
        get() = args[locationIndex]

    val DirectMemoryOp.locationOrNull: Node?
        get() = args.getOrNull(locationIndex)

    

    val LoadField.obj: Node
        get() = args[objIndex]

    val LoadField.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val StoreField.obj: Node
        get() = args[objIndex]

    val StoreField.objOrNull: Node?
        get() = args.getOrNull(objIndex)

    

    val StoreField.value: Node
        get() = args[valueIndex]

    val StoreField.valueOrNull: Node?
        get() = args.getOrNull(valueIndex)

    

    val StoreGlobal.value: Node
        get() = args[valueIndex]

    val StoreGlobal.valueOrNull: Node?
        get() = args.getOrNull(valueIndex)

    

    val AnyInvoke.callArgs: VarArgsList<Node>
        get() = VarArgsList(args, callArgsIndex, Node::class)

    

}



interface ArgumentUpdater : ArgumentAccessor, ArgumentUpdaterBase {
    override var Use.value: Node
        get() = args[valueIndex]
        set(value) { args[valueIndex] = value }

    override var Use.valueOrNull: Node?
        get() = args.getOrNull(valueIndex)
        set(value) { args[valueIndex] = value }

    

    override var Controlled.control: Controlling
        get() = args[controlIndex] as Controlling
        set(value) { args[controlIndex] = value }

    override var Controlled.controlOrNull: Controlling?
        get() = args.getOrNull(controlIndex)?.let { it as Controlling }
        set(value) { args[controlIndex] = value }

    

    override var Return.result: Node
        get() = args[resultIndex]
        set(value) { args[resultIndex] = value }

    override var Return.resultOrNull: Node?
        get() = args.getOrNull(resultIndex)
        set(value) { args[resultIndex] = value }

    

    override var If.cond: Node
        get() = args[condIndex]
        set(value) { args[condIndex] = value }

    override var If.condOrNull: Node?
        get() = args.getOrNull(condIndex)
        set(value) { args[condIndex] = value }

    

    override var IfProjection.owner: If
        get() = args[ownerIndex] as If
        set(value) { args[ownerIndex] = value }

    override var IfProjection.ownerOrNull: If?
        get() = args.getOrNull(ownerIndex)?.let { it as If }
        set(value) { args[ownerIndex] = value }

    

    override var TrueExit.owner: If
        get() = args[ownerIndex] as If
        set(value) { args[ownerIndex] = value }

    override var TrueExit.ownerOrNull: If?
        get() = args.getOrNull(ownerIndex)?.let { it as If }
        set(value) { args[ownerIndex] = value }

    

    override var FalseExit.owner: If
        get() = args[ownerIndex] as If
        set(value) { args[ownerIndex] = value }

    override var FalseExit.ownerOrNull: If?
        get() = args.getOrNull(ownerIndex)?.let { it as If }
        set(value) { args[ownerIndex] = value }

    

    override var Throw.exception: Node
        get() = args[exceptionIndex]
        set(value) { args[exceptionIndex] = value }

    override var Throw.exceptionOrNull: Node?
        get() = args.getOrNull(exceptionIndex)
        set(value) { args[exceptionIndex] = value }

    

    override var Unwind.thrower: Throwing
        get() = args[throwerIndex] as Throwing
        set(value) { args[throwerIndex] = value }

    override var Unwind.throwerOrNull: Throwing?
        get() = args.getOrNull(throwerIndex)?.let { it as Throwing }
        set(value) { args[throwerIndex] = value }

    

    override var AssignVar.assignedValue: Node
        get() = args[assignedValueIndex]
        set(value) { args[assignedValueIndex] = value }

    override var AssignVar.assignedValueOrNull: Node?
        get() = args.getOrNull(assignedValueIndex)
        set(value) { args[assignedValueIndex] = value }

    

    override var Phi.block: BlockEntry
        get() = args[blockIndex] as BlockEntry
        set(value) { args[blockIndex] = value }

    override var Phi.blockOrNull: BlockEntry?
        get() = args.getOrNull(blockIndex)?.let { it as BlockEntry }
        set(value) { args[blockIndex] = value }

    

    override var PhiPlaceholder.block: BlockEntry
        get() = args[blockIndex] as BlockEntry
        set(value) { args[blockIndex] = value }

    override var PhiPlaceholder.blockOrNull: BlockEntry?
        get() = args.getOrNull(blockIndex)?.let { it as BlockEntry }
        set(value) { args[blockIndex] = value }

    

    override var Catch.unwind: Node
        get() = args[unwindIndex]
        set(value) { args[unwindIndex] = value }

    override var Catch.unwindOrNull: Node?
        get() = args.getOrNull(unwindIndex)
        set(value) { args[unwindIndex] = value }

    

    override var BinaryOp.lhs: Node
        get() = args[lhsIndex]
        set(value) { args[lhsIndex] = value }

    override var BinaryOp.lhsOrNull: Node?
        get() = args.getOrNull(lhsIndex)
        set(value) { args[lhsIndex] = value }

    

    override var BinaryOp.rhs: Node
        get() = args[rhsIndex]
        set(value) { args[rhsIndex] = value }

    override var BinaryOp.rhsOrNull: Node?
        get() = args.getOrNull(rhsIndex)
        set(value) { args[rhsIndex] = value }

    

    override var Neg.operand: Node
        get() = args[operandIndex]
        set(value) { args[operandIndex] = value }

    override var Neg.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)
        set(value) { args[operandIndex] = value }

    

    override var Not.operand: Node
        get() = args[operandIndex]
        set(value) { args[operandIndex] = value }

    override var Not.operandOrNull: Node?
        get() = args.getOrNull(operandIndex)
        set(value) { args[operandIndex] = value }

    

    override var NewArray.size: Node
        get() = args[sizeIndex]
        set(value) { args[sizeIndex] = value }

    override var NewArray.sizeOrNull: Node?
        get() = args.getOrNull(sizeIndex)
        set(value) { args[sizeIndex] = value }

    

    override var ThrowingCheck.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }

    override var ThrowingCheck.objOrNull: Node?
        get() = args.getOrNull(objIndex)
        set(value) { args[objIndex] = value }

    

    override var CheckCast.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }

    override var CheckCast.objOrNull: Node?
        get() = args.getOrNull(objIndex)
        set(value) { args[objIndex] = value }

    

    override var TypeInfo.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }

    override var TypeInfo.objOrNull: Node?
        get() = args.getOrNull(objIndex)
        set(value) { args[objIndex] = value }

    

    override var DirectMemoryOp.location: Node
        get() = args[locationIndex]
        set(value) { args[locationIndex] = value }

    override var DirectMemoryOp.locationOrNull: Node?
        get() = args.getOrNull(locationIndex)
        set(value) { args[locationIndex] = value }

    

}


