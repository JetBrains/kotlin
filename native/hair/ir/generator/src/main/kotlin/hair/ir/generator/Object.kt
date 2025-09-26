package hair.ir.generator

import hair.ir.generator.toolbox.Builtin
import hair.ir.generator.toolbox.ModelDSL
import hair.sym.*

object Object : ModelDSL() {

    val memoryAccess by nodeInterface {
        // TODO make nullable for New and others
        param("lastLocationAccess", isVar = true) // FIXME better name? // FIXME better typing?
    }

    // new

    val anyNew by nodeInterface(Builtin.controlFlow, memoryAccess)

//    val anyNewAlloc by abstractClass {
//        interfaces(Builtin.projection, anyNew)
//        param("owner")
//    }

    val new by node(Builtin.spinal) {
        interfaces(anyNew) // FIXME move into alloc??
        formParam("type", Class::class)
        param("lastLocationAccess") // FIXME default?
//        nestedProjection("alloc", "Alloc", anyNewAlloc) {
//            getParam("owner").type = this@node
//        }
    }

    // memory

    val memoryOp by nodeInterface(memoryAccess) {
        formParam("field", MemoryLocation::class)
    }

    val instanceFieldOp by nodeInterface(memoryOp) {
        formParam("field", Field::class)
        param("obj", isVar = true)
    }
    val globalFieldOp by nodeInterface(memoryOp) {
        formParam("field", Global::class)
    }

    val readMemory by nodeInterface(memoryOp)
    val writeMemory by nodeInterface(memoryOp, Builtin.controlFlow) {
        param("value", isVar = true)
    }

    val readField by nodeInterface(readMemory, instanceFieldOp) {
        formParam("field", Field::class)
    }
    val readGlobal by nodeInterface(readMemory, globalFieldOp) {
        formParam("field", Global::class)
    }

    // pinned

    val pinnedMemoryOp by abstractClass(Builtin.spinal) {
        interfaces(memoryOp)
        param("lastLocationAccess")
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

    // floating

    val floatingMemoryRead by abstractClass {
        interfaces(readMemory)
    }

    val readFieldFloating by node(floatingMemoryRead) {
        interfaces(readField)
    }

    val readGlobalFloating by node(floatingMemoryRead) {
        interfaces(readGlobal)
    }

    // type-checks

    val isInstance by node(Builtin.spinal) {
        formParam("type", Class::class)
        param("obj")
    }

    val cast by node(Builtin.spinal) {
        formParam("type", Class::class)
        param("obj")
    }

    // utils

    val indistinctMemory by node {
        variadicParam("inputs")
    }

    val unknown by node()

    val escape by node(Utils.proxyProjection) {
        param("into")
    }

    // TODO use owner node instead of origin arg?
    val overwrite by node(Utils.proxyProjection)

    val neqFilter by node(Utils.proxyProjection) {
        param("to")
    }

}