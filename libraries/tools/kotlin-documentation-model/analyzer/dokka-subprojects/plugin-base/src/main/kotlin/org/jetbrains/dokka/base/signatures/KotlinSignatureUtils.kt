/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.DriOfAny
import org.jetbrains.dokka.links.DriOfUnit
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.ContentKind

public object KotlinSignatureUtils : JvmSignatureUtils {

    private const val classExtension = "::class"
    private val strategy = OnlyOnce
    private val listBrackets = Pair('[', ']')
    private val ignoredAnnotations = setOf(
        /**
         * Rendered separately, see [SinceKotlinTransformer]
         */
        Annotations.Annotation(DRI("kotlin", "SinceKotlin"), emptyMap()),

        /**
         * Rendered separately as its own block, see usage of [ContentKind.Deprecation]
         */
        Annotations.Annotation(DRI("kotlin", "Deprecated"), emptyMap()),
        Annotations.Annotation(DRI("kotlin", "DeprecatedSinceKotlin"), emptyMap()),
        Annotations.Annotation(DRI("java.lang", "Deprecated"), emptyMap()), // could be used as well for interop
    )


    override fun PageContentBuilder.DocumentableContentBuilder.annotationsBlock(d: AnnotationTarget) {
        annotationsBlockWithIgnored(d, ignoredAnnotations, strategy, listBrackets, classExtension)
    }

    override fun PageContentBuilder.DocumentableContentBuilder.annotationsInline(d: AnnotationTarget) {
        annotationsInlineWithIgnored(d, ignoredAnnotations, strategy, listBrackets, classExtension)
    }

    override fun <T : Documentable> WithExtraProperties<T>.modifiers(): SourceSetDependent<Set<ExtraModifiers>> {
        return extra[AdditionalModifiers]?.content?.entries?.associate {
            it.key to it.value.filterIsInstance<ExtraModifiers.KotlinOnlyModifiers>().toSet()
        } ?: emptyMap()
    }

    override fun Annotations.Annotation.isIgnored(): Boolean = this in ignoredAnnotations

    public val PrimitiveJavaType.dri: DRI get() = DRI("kotlin", name.capitalize())

    public val Bound.driOrNull: DRI?
        get() {
            return when (this) {
                is TypeParameter -> dri
                is TypeConstructor -> dri
                is Nullable -> inner.driOrNull
                is DefinitelyNonNullable -> inner.driOrNull
                is PrimitiveJavaType -> dri
                is Void -> DriOfUnit
                is JavaObject -> DriOfAny
                is Dynamic -> null
                is UnresolvedBound -> null
                is TypeAliased -> typeAlias.driOrNull
            }
        }

    public val Projection.drisOfAllNestedBounds: List<DRI> get() = when (this) {
        is TypeParameter -> listOf(dri)
        is TypeConstructor -> listOf(dri) + projections.flatMap { it.drisOfAllNestedBounds }
        is Nullable -> inner.drisOfAllNestedBounds
        is DefinitelyNonNullable -> inner.drisOfAllNestedBounds
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
