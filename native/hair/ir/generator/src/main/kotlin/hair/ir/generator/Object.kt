package hair.ir.generator

import hair.ir.generator.toolbox.*
import hair.sym.*

object Object : ModelDSL() {

    // new

    val anyNew by nodeInterface(DataFlow.valueNode)

    val new by node(ControlFlow.blockBody) {
        interfaces(anyNew)
        formParam("objectType", HairClass::class)
    }

    val newArray by node(ControlFlow.blockBody) {
        interfaces(anyNew)
        formParam("elementType", HairClass::class)
        param("size")
    }

    // type-checks

    val typeCheck by nodeInterface(DataFlow.valueNode) {
        formParam("targetType", HairClass::class)
        param("obj")
    }

    val isInstanceOf by node {
        interfaces(typeCheck)
    }

    // TODO filter/projeciton interface or whatever
    val throwingCheck by abstractClass(ControlFlow.blockBodyWithException) {
        interfaces(DataFlow.valueNode)
        param("obj")
    }

    val checkCast by node(throwingCheck) {
        interfaces(typeCheck)
    }

    val typeInfo by node {
        interfaces(DataFlow.valueNode)
        param("obj")
    }

    val constTypeInfo by node {
        interfaces(Arithmetics.constAny)
        formParam("type", HairClass::class)
    }


    val arraySize by node {
        interfaces(DataFlow.valueNode)
        param("array")
    }

    // TODO sounds like a throwingCheck
    val arrayIndexCheck by node(ControlFlow.blockBodyWithException) {
        param("array")
        param("index")
    }

}
