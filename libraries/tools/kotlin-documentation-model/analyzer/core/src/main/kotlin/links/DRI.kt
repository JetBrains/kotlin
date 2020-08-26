package org.jetbrains.dokka.links

/**
 * [DRI] stands for DokkaResourceIdentifier
 */
data class DRI(
    val packageName: String? = null,
    val classNames: String? = null,
    val callable: Callable? = null,
    val target: DriTarget = PointingToDeclaration,
    val extra: String? = null
) {
    override fun toString(): String =
        "${packageName.orEmpty()}/${classNames.orEmpty()}/${callable?.name.orEmpty()}/${callable?.signature()
            .orEmpty()}/$target/${extra.orEmpty()}"

    companion object {
        val topLevel = DRI()
    }
}

val DriOfUnit = DRI("kotlin", "Unit")
val DriOfAny = DRI("kotlin", "Any")

fun DRI.withClass(name: String) = copy(classNames = if (classNames.isNullOrBlank()) name else "$classNames.$name")

fun DRI.withTargetToDeclaration() = copy(target = PointingToDeclaration)

val DRI.parent: DRI
    get() = when {
        extra != null -> copy(extra = null)
        target != PointingToDeclaration -> copy(target = PointingToDeclaration)
        callable != null -> copy(callable = null)
        classNames != null -> copy(classNames = classNames.substringBeforeLast(".", "").takeIf { it.isNotBlank() })
        else -> DRI.topLevel
    }

val DRI.sureClassNames
    get() = classNames ?: throw IllegalStateException("Malformed DRI. It requires classNames in this context.")

data class Callable(
    val name: String,
    val receiver: TypeReference? = null,
    val params: List<TypeReference>
) {
    fun signature() = "${receiver?.toString().orEmpty()}#${params.joinToString("#")}"

    companion object
}

sealed class TypeReference {
    companion object
}

data class JavaClassReference(val name: String) : TypeReference() {
    override fun toString(): String = name
}

data class TypeParam(val bounds: List<TypeReference>) : TypeReference()

data class TypeConstructor(
    val fullyQualifiedName: String,
    val params: List<TypeReference>
) : TypeReference() {
    override fun toString() = fullyQualifiedName +
            (if (params.isNotEmpty()) "[${params.joinToString(",")}]" else "")
}

data class RecursiveType(val rank: Int): TypeReference() {
    override fun toString() = "^".repeat(rank + 1)
}

data class Nullable(val wrapped: TypeReference) : TypeReference() {
    override fun toString() = "$wrapped?"
}

object StarProjection : TypeReference() {
    override fun toString() = "*"
}

sealed class DriTarget {
    override fun toString(): String = this.javaClass.simpleName

    companion object
}

data class PointingToGenericParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToGenericParameters($parameterIndex)"
}

object PointingToDeclaration : DriTarget()

data class PointingToCallableParameters(val parameterIndex: Int) : DriTarget() {
    override fun toString(): String = "PointingToCallableParameters($parameterIndex)"
}

fun DriTarget.nextTarget(): DriTarget = when (this) {
    is PointingToGenericParameters -> PointingToGenericParameters(this.parameterIndex + 1)
    is PointingToCallableParameters -> PointingToCallableParameters(this.parameterIndex + 1)
    else -> this
}
