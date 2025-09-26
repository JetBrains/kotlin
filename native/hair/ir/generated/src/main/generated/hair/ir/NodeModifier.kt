package hair.ir

import hair.ir.nodes.*
import hair.sym.*
import hair.sym.Type.Primitive

val MemoryAccess.lastLocationAccess: Node
    get() = args[lastLocationAccessIndex]

val InstanceFieldOp.obj: Node
    get() = args[objIndex]

val WriteMemory.value: Node
    get() = args[valueIndex]


val Placeholder.inputs: VarArgsList<Node>
    get() = VarArgsList(args, 0, Node::class)

val Use.value: Node
    get() = args[valueIndex]

val ProxyProjection.owner: ControlFlow
    get() = args[ownerIndex] as ControlFlow

val ProxyProjection.origin: Node
    get() = args[originIndex]

val If.condition: Node
    get() = args[conditionIndex]

val Throw.exception: Node
    get() = args[exceptionIndex]

val Catch.xBlock: XBlock
    get() = args[xBlockIndex] as XBlock

val Catch.catchedValues: VarArgsList<Node>
    get() = VarArgsList(args, 1, Node::class)

val AssignVar.assignedValue: Node
    get() = args[assignedValueIndex]

val Phi.block: Block
    get() = args[blockIndex] as Block

val Phi.joinedValues: VarArgsList<Node>
    get() = VarArgsList(args, 1, Node::class)

val BinaryOp.lhs: Node
    get() = args[lhsIndex]

val BinaryOp.rhs: Node
    get() = args[rhsIndex]

val New.lastLocationAccess: Node
    get() = args[lastLocationAccessIndex]

val PinnedMemoryOp.lastLocationAccess: Node
    get() = args[lastLocationAccessIndex]

val PinnedInstanceFieldOp.obj: Node
    get() = args[objIndex]

val WriteField.value: Node
    get() = args[valueIndex]

val WriteGlobal.value: Node
    get() = args[valueIndex]

val ReadFieldFloating.lastLocationAccess: Node
    get() = args[lastLocationAccessIndex]

val ReadFieldFloating.obj: Node
    get() = args[objIndex]

val ReadGlobalFloating.lastLocationAccess: Node
    get() = args[lastLocationAccessIndex]

val IsInstance.obj: Node
    get() = args[objIndex]

val Cast.obj: Node
    get() = args[objIndex]

val IndistinctMemory.inputs: VarArgsList<Node>
    get() = VarArgsList(args, 0, Node::class)

val Escape.into: Node
    get() = args[intoIndex]

val NeqFilter.to: Node
    get() = args[toIndex]

val StaticCall.lastLocationAccess: Node
    get() = args[lastLocationAccessIndex]

val StaticCall.callArgs: VarArgsList<Node>
    get() = VarArgsList(args, 1, Node::class)

val Return.lastLocationAccess: Node
    get() = args[lastLocationAccessIndex]

val Return.value: Node
    get() = args[valueIndex]


interface ArgsAccessor

interface ArgsModifier : ArgsUpdater {
    var MemoryAccess.lastLocationAccess: Node
        get() = args[lastLocationAccessIndex]
        set(value) { args[lastLocationAccessIndex] = value }
    
    var InstanceFieldOp.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }
    
    var WriteMemory.value: Node
        get() = args[valueIndex]
        set(value) { args[valueIndex] = value }
    
    
    val Placeholder.inputs: VarArgsList<Node>
        get() = VarArgsList(args, 0, Node::class)
    
    var Use.value: Node
        get() = args[valueIndex]
        set(value) { args[valueIndex] = value }
    
    var ProxyProjection.owner: ControlFlow
        get() = args[ownerIndex] as ControlFlow
        set(value) { args[ownerIndex] = value }
    
    var ProxyProjection.origin: Node
        get() = args[originIndex]
        set(value) { args[originIndex] = value }
    
    var If.condition: Node
        get() = args[conditionIndex]
        set(value) { args[conditionIndex] = value }
    
    var Throw.exception: Node
        get() = args[exceptionIndex]
        set(value) { args[exceptionIndex] = value }
    
    var Catch.xBlock: XBlock
        get() = args[xBlockIndex] as XBlock
        set(value) { args[xBlockIndex] = value }
    
    val Catch.catchedValues: VarArgsList<Node>
        get() = VarArgsList(args, 1, Node::class)
    
    var AssignVar.assignedValue: Node
        get() = args[assignedValueIndex]
        set(value) { args[assignedValueIndex] = value }
    
    var Phi.block: Block
        get() = args[blockIndex] as Block
        set(value) { args[blockIndex] = value }
    
    val Phi.joinedValues: VarArgsList<Node>
        get() = VarArgsList(args, 1, Node::class)
    
    var BinaryOp.lhs: Node
        get() = args[lhsIndex]
        set(value) { args[lhsIndex] = value }
    
    var BinaryOp.rhs: Node
        get() = args[rhsIndex]
        set(value) { args[rhsIndex] = value }
    
    var New.lastLocationAccess: Node
        get() = args[lastLocationAccessIndex]
        set(value) { args[lastLocationAccessIndex] = value }
    
    var PinnedMemoryOp.lastLocationAccess: Node
        get() = args[lastLocationAccessIndex]
        set(value) { args[lastLocationAccessIndex] = value }
    
    var PinnedInstanceFieldOp.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }
    
    var WriteField.value: Node
        get() = args[valueIndex]
        set(value) { args[valueIndex] = value }
    
    var WriteGlobal.value: Node
        get() = args[valueIndex]
        set(value) { args[valueIndex] = value }
    
    var ReadFieldFloating.lastLocationAccess: Node
        get() = args[lastLocationAccessIndex]
        set(value) { args[lastLocationAccessIndex] = value }
    
    var ReadFieldFloating.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }
    
    var ReadGlobalFloating.lastLocationAccess: Node
        get() = args[lastLocationAccessIndex]
        set(value) { args[lastLocationAccessIndex] = value }
    
    var IsInstance.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }
    
    var Cast.obj: Node
        get() = args[objIndex]
        set(value) { args[objIndex] = value }
    
    val IndistinctMemory.inputs: VarArgsList<Node>
        get() = VarArgsList(args, 0, Node::class)
    
    var Escape.into: Node
        get() = args[intoIndex]
        set(value) { args[intoIndex] = value }
    
    var NeqFilter.to: Node
        get() = args[toIndex]
        set(value) { args[toIndex] = value }
    
    var StaticCall.lastLocationAccess: Node
        get() = args[lastLocationAccessIndex]
        set(value) { args[lastLocationAccessIndex] = value }
    
    val StaticCall.callArgs: VarArgsList<Node>
        get() = VarArgsList(args, 1, Node::class)
    
    var Return.lastLocationAccess: Node
        get() = args[lastLocationAccessIndex]
        set(value) { args[lastLocationAccessIndex] = value }
    
    var Return.value: Node
        get() = args[valueIndex]
        set(value) { args[valueIndex] = value }
    
}

