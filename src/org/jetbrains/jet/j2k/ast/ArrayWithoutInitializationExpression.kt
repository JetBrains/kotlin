package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.ArrayType
import org.jetbrains.jet.j2k.ast.types.Type

public open class ArrayWithoutInitializationExpression(val `type` : Type, val expressions : List<Expression>) : Expression() {
    public override fun toKotlin() : String {
        if (`type` is ArrayType) {
            return constructInnerType(`type`, expressions)
        }

        return getConstructorName(`type`)
    }

    private fun constructInnerType(hostType : ArrayType, expressions: List<Expression>) : String {
        if (expressions.size() == 1) {
            return oneDim(hostType, expressions[0])
        }

        val innerType = hostType.elementType
        if (expressions.size() > 1 && innerType is ArrayType) {
            return oneDim(hostType, expressions[0], "{" + constructInnerType(innerType, expressions.subList(1, expressions.size())) + "}")
        }

        return getConstructorName(hostType)
    }

    class object {
        private open fun oneDim(`type` : Type, size : Expression) : String {
            return oneDim(`type`, size, "")
        }

        private open fun oneDim(`type` : Type, size : Expression, init : String) : String {
            return getConstructorName(`type`) + "(" + size.toKotlin() + init.withPrefix(", ") + ")"
        }

        private open fun getConstructorName(`type` : Type) : String = `type`.convertedToNotNull().toKotlin()
    }
}
