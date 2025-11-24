package hair.ir.generator

import hair.ir.generator.toolbox.*
import hair.sym.*

object Memory : ModelDSL() {

    val memoryOp by nodeInterface()

    val anyLoad by nodeInterface(memoryOp)
    val anyStore by nodeInterface(memoryOp) {
        param("value")
    }

    val directMemoryOp by abstractClass {
        interfaces(memoryOp)
        formParam("type", HairType::class)
        param("location")
    }

    val load by node(directMemoryOp) {
        interfaces(anyLoad)
    }

    val store by node(directMemoryOp) {
        interfaces(anyLoad)
        // param("value")
    }


    val instanceFieldOp by nodeInterface(memoryOp) {
        formParam("field", Field::class)
        param("obj")
    }

    val loadField by node {
        interfaces(instanceFieldOp, anyLoad)
    }

    val storeField by node {
        interfaces(instanceFieldOp, anyStore)
    }


    val globalOp by nodeInterface(memoryOp) {
        formParam("field", Global::class)
    }

    val loadGlobal by node {
        interfaces(globalOp, anyLoad)
    }

    val storeGlobal by node {
        interfaces(globalOp, anyStore)
    }

    // TODO floating loads

}