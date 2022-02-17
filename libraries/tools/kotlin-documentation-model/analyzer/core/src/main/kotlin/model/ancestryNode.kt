package org.jetbrains.dokka.model

data class AncestryNode(
    val typeConstructor: TypeConstructor,
    val superclass: AncestryNode?,
    val interfaces: List<AncestryNode>,
) {
    fun allImplementedInterfaces(): List<TypeConstructor> {
        fun traverseInterfaces(ancestry: AncestryNode): List<TypeConstructor> =
            ancestry.interfaces.flatMap { listOf(it.typeConstructor) + traverseInterfaces(it) } +
                    (ancestry.superclass?.let(::traverseInterfaces) ?: emptyList())
        return traverseInterfaces(this).distinct()
    }
}
