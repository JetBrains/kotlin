/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.sourceSet

internal class SuppressedByAnnotationsDocumentableFilterTransformer(
    context: DokkaContext
) : SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val suppressAnnotatedWith = sourceSet(d).suppressAnnotatedWith
        if (suppressAnnotatedWith.isEmpty()) return false

        val annotations = (d as? WithExtraProperties<*>)?.annotations() ?: return false
        return annotations.any { annotation ->
            val fqName = listOfNotNull(annotation.dri.packageName, annotation.dri.classNames).joinToString(".")
            fqName in suppressAnnotatedWith
        }
    }

    private fun WithExtraProperties<*>.annotations(): List<Annotations.Annotation>? {
        return this.extra.allOfType<Annotations>().flatMap { annotations ->
            annotations.directAnnotations.values.singleOrNull() ?: emptyList()
        }.takeIf { it.isNotEmpty() }
    }
}
