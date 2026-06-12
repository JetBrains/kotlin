package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.ArithmeticType
import hair.sym.CmpOp
import hair.sym.HairType

object Arithmetics : ModelDSL() {

    val constAny by nodeInterface(Values.valueNode) {
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

    val `null` by node {
        interfaces(constAny)
    }

    val binaryOp by abstractClass {
        param("lhs")
        param("rhs")
    }

    // Marker interface for the arithmetic binary ops. Declares `opType:
    // ArithmeticType` as a form param so it can be read uniformly without
    // dispatching on the concrete class.
    val arithBinaryOp by nodeInterface(Values.valueNode) {
        formParam("opType", ArithmeticType::class)
    }

    val add by node(binaryOp) { interfaces(arithBinaryOp) }
    val sub by node(binaryOp) { interfaces(arithBinaryOp) }
    val mul by node(binaryOp) { interfaces(arithBinaryOp) }
    val div by node(binaryOp) { interfaces(arithBinaryOp) }
    val rem by node(binaryOp) { interfaces(arithBinaryOp) }

    // TODO
    val and by node(binaryOp) { interfaces(arithBinaryOp) }
    val or by node(binaryOp) { interfaces(arithBinaryOp) }
    val xor by node(binaryOp) { interfaces(arithBinaryOp) }
    val shl by node(binaryOp) { interfaces(arithBinaryOp) }
    val shr by node(binaryOp) { interfaces(arithBinaryOp) }
    val ushr by node(binaryOp) { interfaces(arithBinaryOp) }

    val Neg by node {
        interfaces(Values.valueNode)
        param("operand")
    }

    // FIXME not exactly arithmetics:
    // `type` here is the *operand* type — Cmp accepts REFERENCE as well as the
    // arithmetic widths. The result type (always INT) is reported by valueType.
    val cmp by node(binaryOp) {
        interfaces(Values.valueNode)
        formParam("type", HairType::class)
        formParam("op", CmpOp::class)
    }

    val not by node {
        interfaces(Values.valueNode)
        param("operand")
    }

    // Interface (not abstractClass) so `targetType` and `operand` are visible
    // when we hold a value statically typed as `Cast`.
    val cast by nodeInterface(Values.valueNode) {
        formParam("targetType", HairType::class)
        param("operand")
    }

    val signExtend by node { interfaces(cast) }
    val zeroExtend by node { interfaces(cast) }
    val truncate by node { interfaces(cast) }
    val reinterpret by node { interfaces(cast) }

}
