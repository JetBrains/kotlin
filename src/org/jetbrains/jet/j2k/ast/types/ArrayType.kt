package org.jetbrains.jet.j2k.ast.types

public open class ArrayType(val elementType : Type, nullable: Boolean) : Type(nullable) {
    public override fun toKotlin() : String {
        if (elementType is PrimitiveType) {
            return elementType.toKotlin() + "Array" + isNullableStr()
        }

        return "Array<" + elementType.toKotlin() + ">" + isNullableStr()
    }

    public override fun convertedToNotNull() : Type = ArrayType(elementType, false)
}
