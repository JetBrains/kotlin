package hair.ir.generator

import hair.ir.generator.toolbox.*
import hair.sym.*

object Object : ModelDSL() {

    // new

    val anyNew by nodeInterface()

    val new by node(ControlFlow.blockBody) {
        interfaces(anyNew)
        formParam("objectType", Class::class)
    }

    // memory

    val memoryOp by nodeInterface {
        formParam("field", MemoryLocation::class)
    }

    val instanceFieldOp by nodeInterface(memoryOp) {
        formParam("field", Field::class)
        param("obj", variable = true)
    }
    val globalFieldOp by nodeInterface(memoryOp) {
        formParam("field", Global::class)
    }

    val readMemory by nodeInterface(memoryOp)
    val writeMemory by nodeInterface(memoryOp) {
        param("value", variable = true)
    }

    val readField by nodeInterface(readMemory, instanceFieldOp) {
        formParam("field", Field::class)
    }
    val readGlobal by nodeInterface(readMemory, globalFieldOp) {
        formParam("field", Global::class)
    }

    // pinned

    val pinnedMemoryOp by abstractClass(ControlFlow.blockBody) {
        interfaces(memoryOp)
    }

    val pinnedInstanceFieldOp by abstractClass(pinnedMemoryOp) {
        interfaces(instanceFieldOp)
        inheritAll()
    }

    val pinnedGlobalFieldOp by abstractClass(pinnedMemoryOp) {
        interfaces(globalFieldOp)
        inheritAll()
    }

    val readFieldPinned by node(pinnedInstanceFieldOp) {
        interfaces(readField)
    }

    val readGlobalPinned by node(pinnedGlobalFieldOp) {
        interfaces(readGlobal)
    }

    val writeField by node(pinnedInstanceFieldOp) {
        interfaces(writeMemory)
    }

    val writeGlobal by node(pinnedGlobalFieldOp) {
        interfaces(writeMemory)
    }

    // type-checks

    val typeCheck by abstractClass {
        formParam("targetType", Type.Reference::class)
        param("obj")
    }

    val isInstanceOf by node(typeCheck)

    val checkCast by node(typeCheck)


    // utils

    val valueProxy by abstractClass(ControlFlow.blockBody) {
        param("origin")
    }

}