package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfAny
import org.jetbrains.dokka.links.DriOfUnit
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.properties.WithExtraProperties

object KotlinSignatureUtils : JvmSignatureUtils {

    private val strategy = OnlyOnce
    private val listBrackets = Pair('[', ']')
    private val classExtension = "::class"
    private val ignoredAnnotations = setOf(
        Annotations.Annotation(DRI("kotlin", "SinceKotlin"), emptyMap()),
        Annotations.Annotation(DRI("kotlin", "Deprecated"), emptyMap())
    )


    override fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: AnnotationTarget) =
        annotationsBlockWithIgnored(d, ignoredAnnotations, strategy, listBrackets, classExtension)

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: AnnotationTarget) =
        annotationsInlineWithIgnored(d, ignoredAnnotations, strategy, listBrackets, classExtension)

    override fun <T : Documentable> WithExtraProperties<T>.modifiers() =
        extra[AdditionalModifiers]?.content?.entries?.map {
            it.key to it.value.filterIsInstance<ExtraModifiers.KotlinOnlyModifiers>().toSet()
        }?.toMap() ?: emptyMap()


    val PrimitiveJavaType.dri: DRI get() = DRI("kotlin", name.capitalize())

    val Bound.driOrNull: DRI?
        get() {
            return when (this) {
                is TypeParameter -> dri
                is TypeConstructor -> dri
                is Nullable -> inner.driOrNull
                is PrimitiveJavaType -> dri
                is Void -> DriOfUnit
                is JavaObject -> DriOfAny
                is Dynamic -> null
                is UnresolvedBound -> null
                is TypeAliased -> typeAlias.driOrNull
            }
        }

    val Projection.drisOfAllNestedBounds: List<DRI> get() = when (this) {
        is TypeParameter -> listOf(dri)
        is TypeConstructor -> listOf(dri) + projections.flatMap { it.drisOfAllNestedBounds }
        is Nullable -> inner.drisOfAllNestedBounds
        is PrimitiveJavaType -> listOf(dri)
        is Void -> listOf(DriOfUnit)
        is JavaObject -> listOf(DriOfAny)
        is Dynamic -> emptyList()
        is UnresolvedBound -> emptyList()
        is Variance<*> -> inner.drisOfAllNestedBounds
        is Star -> emptyList()
        is TypeAliased -> listOfNotNull(typeAlias.driOrNull, inner.driOrNull)
    }

}
