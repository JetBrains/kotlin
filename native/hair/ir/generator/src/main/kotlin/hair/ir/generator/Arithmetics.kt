package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.CmpOp
import hair.sym.HairType

object Arithmetics : ModelDSL() {

    val constAny by nodeInterface {
        //formParam("value", Any::class) // TODO nullable?
    }

    val constI by node {
        interfaces(constAny)
        formParam("value", Int::class)
    }

    val constL by node {
        interfaces(constAny)
        formParam("value", Long::class)
    }

    val constF by node {
        interfaces(constAny)
        formParam("value", Float::class)
    }

    val constD by node {
        interfaces(constAny)
        formParam("value", Double::class)
    }

    val `true` by node {
        interfaces(constAny)
    }

    val `false` by node {
        interfaces(constAny)
    }

    val `null` by node {
        interfaces(constAny)
    }

    val binaryOp by abstractClass {
        formParam("type", HairType::class)
        param("lhs")
        param("rhs")
    }

    val add by node(binaryOp)
    val sub by node(binaryOp)
    val mul by node(binaryOp)
    val div by node(binaryOp)
    val rem by node(binaryOp)

    // TODO
    val and by node(binaryOp)
    val or by node(binaryOp)
    val xor by node(binaryOp)
    val shl by node(binaryOp)
    val shr by node(binaryOp)
    val ushr by node(binaryOp)

    // FIXME not exactly arithmetics:
    val cmp by node(binaryOp) {
        formParam("op", CmpOp::class)
    }

    val not by node {
        param("operand")
    }

    val cast by abstractClass {
        formParam("targetType", HairType::class)
        param("operand")
    }

    val signExtend by node(cast)
    val zeroExtend by node(cast)
    val truncate by node(cast)
    val reinterpret by node(cast)

}