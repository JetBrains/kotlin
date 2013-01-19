package org.jetbrains.jet.j2k.ast

import org.jetbrains.jet.j2k.ast.types.Type

public open class MethodCallExpression(val methodCall: Expression,
                                       val arguments: List<Expression>,
                                       val typeParameters: List<Type>,
                                       val resultIsNullable: Boolean = false): Expression() {
    public override fun isNullable(): Boolean = methodCall.isNullable() || resultIsNullable

    public override fun toKotlin(): String {
        val typeParamsToKotlin: String = typeParameters.toKotlin(", ", "<", ">")
        val argumentsMapped = arguments.map { it.toKotlin() }
        return methodCall.toKotlin() + typeParamsToKotlin + "(" + argumentsMapped.makeString(", ") + ")"
    }

    class object {
        fun build(receiver: Expression, methodName: String, arguments: List<Expression> = arrayList()): MethodCallExpression {
            return MethodCallExpression(CallChainExpression(receiver, Identifier(methodName, false)),
                    arguments,
                    arrayList(), false)
        }
    }
}
