package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.ArithmeticType
import hair.sym.CmpOp
import hair.sym.HairType

object Arithmetics : ModelDSL() {

    val constAny by nodeInterface(DataFlow.valueNode) {
        //formParam("value", Any::class) // TODO nullable?
    }

    val const by node {
        interfaces(constAny)
        formParam("value", Number::class)
    }

    val `null` by node {
        interfaces(constAny)
    }

    val constBoolean by abstractClass {
        interfaces(constAny)
    }

    val `true` by node(constBoolean)
    val `false` by node(constBoolean)

    val binaryOp by abstractClass {
        interfaces(DataFlow.valueNode)
        param("lhs")
        param("rhs")
    }

    val arithBinaryOp by abstractClass(binaryOp) {
        formParam("opType", ArithmeticType::class)
    }

    val add by node(arithBinaryOp)
    val sub by node(arithBinaryOp)
    val mul by node(arithBinaryOp)
    val div by node(arithBinaryOp)
    val rem by node(arithBinaryOp)

    val neg by node {
        param("operand")
    }

    // TODO
    val and by node(arithBinaryOp)
    val or by node(arithBinaryOp)
    val xor by node(arithBinaryOp)
    val shl by node(arithBinaryOp)
    val shr by node(arithBinaryOp)
    val ushr by node(arithBinaryOp)

    val inv by node {
        interfaces(DataFlow.valueNode)
        param("operand")
    }

    // FIXME not exactly arithmetics:
    val cmp by node(binaryOp) {
        formParam("type", HairType::class)
        formParam("op", CmpOp::class)
    }

    // Boolean negation
    val not by node {
        interfaces(DataFlow.valueNode)
        param("operand")
    }

    val cast by abstractClass {
        interfaces(DataFlow.valueNode)
        formParam("targetType", HairType::class)
        param("operand")
    }

    val signExtend by node(cast)
    val zeroExtend by node(cast)
    val truncate by node(cast)
    val reinterpret by node(cast)

}
