package hair.ir.generator

import hair.ir.generator.toolbox.ModelDSL
import hair.sym.ArithmeticType
import kotlin.reflect.KClass

object Arithmetics : ModelDSL() {

    val constAny by nodeInterface {
        formParam("value", Any::class) // TODO nullable?
    }

    private enum class ArithmeticType(val suffix: String, val cls: KClass<out Number>, val isFloating: Boolean) {
        INT("I", Int::class, false),
        LONG("L", Long::class, false),
        FLOAT("F", Float::class, true),
        DOUBLE("D", Double::class, true),
        // TODO i128?
    }

    val binaryOp by abstractClass {
        param("lhs")
        param("rhs")
    }

    val associativeOp by nodeInterface()
    val commutativeOp by nodeInterface()

    init {
        for (type in ArithmeticType.entries) {
            val suffix = type.suffix

            val const by node(explicitName = "Const$suffix") {
                interfaces(constAny)
                formParam("value", type.cls)
            }

            val binaryOpX by abstractClass(binaryOp, explicitName = "BinaryOp$suffix")

            val addX by node(binaryOpX, explicitName = "Add$suffix") {
                interfaces(commutativeOp)
                if (!type.isFloating) {
                    interfaces(associativeOp)
                }
            }

            val subX by node(binaryOpX, explicitName = "Sub$suffix") {
                if (!type.isFloating) {
                    interfaces(associativeOp)
                }
            }

            val mulX by node(binaryOpX, explicitName = "Mul$suffix") {
                interfaces(commutativeOp)
                if (!type.isFloating) {
                    interfaces(associativeOp)
                }
            }

            val divX by node(binaryOpX, explicitName = "Div$suffix")

            val remX by node(binaryOpX, explicitName = "Rem$suffix")
        }
    }


    // TODO
    // and
    // or
    // xor
    // shl
    // shr
    // ushr

}