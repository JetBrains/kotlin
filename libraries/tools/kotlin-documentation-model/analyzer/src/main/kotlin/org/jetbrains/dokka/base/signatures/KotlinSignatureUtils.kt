/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.base.transformers.pages.annotations.SinceKotlinTransformer
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.kotlin.documentation.KdElementId

internal object KotlinSignatureUtils : JvmSignatureUtils {

    private val ignoredAnnotationIds = setOf(
        /**
         * Rendered separately, see [SinceKotlinTransformer]
         */
        KdElementId("kotlin/SinceKotlin"),
        /**
         * Rendered separately as its own block, see usage of [ContentKind.Deprecation]
         */
        KdElementId("kotlin/Deprecated"),
        KdElementId("kotlin/DeprecatedSinceKotlin"),
        KdElementId("java.lang/Deprecated"), // could be used as well for interop
    )

    override fun <T : Documentable> WithExtraProperties<T>.modifiers(): SourceSetDependent<Set<ExtraModifiers>> {
        return extra[AdditionalModifiers]?.content?.entries?.associate {
            it.key to it.value.filterIsInstance<ExtraModifiers.KotlinOnlyModifiers>().toSet()
        } ?: emptyMap()
    }

    override fun Annotations.Annotation.isIgnored(): Boolean =
        classLikeId in ignoredAnnotationIds && params.isEmpty() && !mustBeDocumented && scope == Annotations.AnnotationScope.DIRECT


}
