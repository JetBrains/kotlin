package hair.ir.generator

import hair.ir.generator.toolbox.*
import hair.sym.*

object Object : ModelDSL() {

    // new

    val anyNew by nodeInterface()

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

    val typeCheck by nodeInterface {
        formParam("targetType", HairClass::class)
        param("obj")
    }

    val isInstanceOf by node {
        interfaces(typeCheck)
    }

    // TODO filter/projeciton interface or whatever
    val throwingCheck by abstractClass(ControlFlow.blockBodyWithException) {
        param("obj")
    }

    val checkCast by node(throwingCheck) {
        interfaces(typeCheck)
    }

    val typeInfo by node {
        param("obj")
    }

    val constTypeInfo by node {
        interfaces(Arithmetics.constAny)
        formParam("type", HairClass::class)
    }

}