package hair.ir.generator

import hair.ir.generator.toolbox.*
import hair.sym.*

object Memory : ModelDSL() {

    val memoryOp by nodeInterface()

    val anyLoad by nodeInterface(memoryOp, DataFlow.valueNode)
    val anyStore by nodeInterface(memoryOp) {
        param("value")
    }

    val pinnedMemoryOp by abstractClass(ControlFlow.blockBody) {
        interfaces(memoryOp)
    }

    val directMemoryOp by abstractClass(pinnedMemoryOp) {
        interfaces(memoryOp)
        formParam("type", HairType::class)
        param("location")
    }

    val load by node(directMemoryOp) {
        interfaces(anyLoad)
    }

    val store by node(directMemoryOp) {
        interfaces(anyStore)
        param("value")
    }


    val instanceFieldOp by nodeInterface(memoryOp) {
        formParam("field", Field::class)
        param("obj")
    }

    val loadField by node(pinnedMemoryOp) {
        interfaces(instanceFieldOp, anyLoad)
    }

    val storeField by node(pinnedMemoryOp) {
        interfaces(instanceFieldOp, anyStore)
    }


    val globalOp by nodeInterface(memoryOp) {
        formParam("field", Global::class)
    }

    val loadGlobal by node(pinnedMemoryOp) {
        interfaces(globalOp, anyLoad)
    }

    val storeGlobal by node(pinnedMemoryOp) {
        interfaces(globalOp, anyStore)
    }


    val arrayMemoryOp by abstractClass(pinnedMemoryOp) {
        formParam("elementType", HairType::class)
        param("array")
        param("index")
    }

    val loadArrayElement by node(arrayMemoryOp) {
        interfaces(anyLoad)
    }

    val storeArrayElement by node(arrayMemoryOp) {
        interfaces(anyStore)
    }

    // TODO floating loads

}
