package org.jetbrains.idl2k

import java.util.*

sealed class Type {
    abstract val nullable: Boolean
    abstract fun render(): String
}

private fun String.appendNullabilitySuffix(type: Type) = if (type.nullable) "$this?" else this

object UnitType : Type() {
    override val nullable: Boolean
        get() = false

    override fun render() = "Unit"
}
object DynamicType : Type() {
    override val nullable: Boolean
        get() = false

    override fun render() = "dynamic"
}
data class AnyType(override val nullable: Boolean = true) : Type() {
    override fun render() = "Any".appendNullabilitySuffix(this)
}
data class SimpleType(val type: String, override val nullable: Boolean) : Type() {
    override fun render() = type.appendNullabilitySuffix(this)
}
data class FunctionType(val parameterTypes : List<Attribute>, val returnType : Type, override val nullable: Boolean) : Type() {
    override fun render() = if (nullable) "(${renderImpl()})?" else renderImpl()
    private fun renderImpl() = "(${parameterTypes.joinToString(", ") { it.type.render() }}) -> ${returnType.render()}"
}
data class PromiseType(val valueType: Type, override val nullable: Boolean) : Type() {
    override fun render() = "Promise<${valueType.render()}>".appendNullabilitySuffix(this)
}

val FunctionType.arity : Int
    get() = parameterTypes.size

class UnionType(val namespace: String, types: Collection<Type>, override val nullable: Boolean) : Type() {
    val memberTypes: Set<Type> = LinkedHashSet(types.sortedBy { it.toString() })
    val name = "Union${this.memberTypes.map { it.render() }.joinToString("Or")}"

    operator fun contains(type: Type) = type in memberTypes
    override fun equals(other: Any?): Boolean = other is UnionType && memberTypes == other.memberTypes
    override fun hashCode(): Int = memberTypes.hashCode()
    override fun toString(): String = memberTypes.joinToString(", ", "Union<", ">")

    override fun render(): String = name.appendNullabilitySuffix(this)

    fun copy(namespace: String = this.namespace, types: Collection<Type> = this.memberTypes, nullable: Boolean = this.nullable) =
            UnionType(namespace, types, nullable)
}

fun UnionType.toSingleTypeIfPossible() = if (this.memberTypes.size == 1) this.memberTypes.single().withNullability(nullable) else this

data class ArrayType(val memberType: Type, val mutable: Boolean, override val nullable: Boolean) : Type() {
    override fun render(): String = "Array<${if (mutable) "" else "out "}${memberType.render()}>".appendNullabilitySuffix(this)
}

@Suppress("UNCHECKED_CAST")
private fun <T: Type> T.copyWithNullability(nullable: Boolean): T = when (this) {
    is UnitType -> UnitType
    is DynamicType -> this

    is AnyType -> this.copy(nullable = nullable)
    is SimpleType -> this.copy(nullable = nullable)
    is FunctionType -> this.copy(nullable = nullable)
    is UnionType -> this.copy(types = this.memberTypes, nullable = nullable)
    is ArrayType -> this.copy(nullable = nullable)
    is PromiseType -> this.copy(nullable = nullable)
    else -> throw UnsupportedOperationException()
} as T


private fun <T: Type> T.withNullabilityImpl(nullable: Boolean): T = if (this.nullable == nullable) this else copyWithNullability(nullable)
fun <T: Type> T.withNullability(nullable: Boolean): T = withNullabilityImpl(this.nullable or nullable)
fun <T: Type> T.toNullable(): T = withNullabilityImpl(true)
fun <T: Type> T.dropNullable(): T = withNullabilityImpl(false)

@Suppress("UNCHECKED_CAST")
fun <T: Type> T.toNullableIfNonPrimitive(): T = when (this) {
    is UnitType -> UnitType
    is DynamicType -> DynamicType

    is SimpleType -> when (this.type) {
        "Int", "Short", "Byte", "Float", "Double", "Boolean", "Long" -> this
        else -> this.toNullable()
    }
    else -> this.toNullable()
} as T