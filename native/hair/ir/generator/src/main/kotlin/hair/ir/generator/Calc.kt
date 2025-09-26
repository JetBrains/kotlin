package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.Type.Primitive

object Calc : ModelDSL() {

    val constInt by node {
        formParam("value", Long::class)
    }

    val constFloat by node {
        formParam("value", Double::class)
    }

    val arithmeticOp by abstractClass {
        formParam("type", Primitive::class)
    }

    val binaryOp by abstractClass(arithmeticOp) {
        param("lhs")
        param("rhs")
    }

    val associativeOp by nodeInterface()
    val commutativeOp by nodeInterface()

    val add by node(binaryOp) {
        interfaces(associativeOp, commutativeOp)
    }

    val sub by node(binaryOp) {
        interfaces(associativeOp)
    }

    val mul by node(binaryOp) {
        interfaces(associativeOp, commutativeOp)
    }

    val div by node(binaryOp)

    val rem by node(binaryOp)

    // FIXME is it arythmetic? does it need the type?
    val and by node(binaryOp) {
        interfaces(associativeOp, commutativeOp)
    }
    val or by node(binaryOp) {
        interfaces(associativeOp, commutativeOp)
    }
    val xor by node(binaryOp) {
        interfaces(associativeOp, commutativeOp)
    }

    // TODO separate class?
    val shl by node(binaryOp)
    val shr by node(binaryOp)
    val ushr by node(binaryOp)

}