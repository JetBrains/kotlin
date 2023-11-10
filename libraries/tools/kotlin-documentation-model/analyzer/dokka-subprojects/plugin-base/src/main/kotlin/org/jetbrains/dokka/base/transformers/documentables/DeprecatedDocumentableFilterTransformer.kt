/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaConfiguration.PackageOptions
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.EnumValue
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.perPackageOptions
import org.jetbrains.dokka.transformers.documentation.sourceSet

/**
 * If [PackageOptions.skipDeprecated] or [DokkaConfiguration.DokkaSourceSet.skipDeprecated] is set
 * to `true`, suppresses documentables marked with [kotlin.Deprecated] or [java.lang.Deprecated].
 * Package options are given preference over global options.
 *
 * Documentables with [kotlin.Deprecated.level] set to [DeprecationLevel.HIDDEN]
 * are suppressed regardless of global and package options.
 */
public class DeprecatedDocumentableFilterTransformer(
    context: DokkaContext
) : SuppressedByConditionDocumentableFilterTransformer(context) {

    override fun shouldBeSuppressed(d: Documentable): Boolean {
        val annotations = (d as? WithExtraProperties<*>)?.annotations() ?: return false
        if (annotations.isEmpty())
            return false

        val deprecatedAnnotations = filterDeprecatedAnnotations(annotations)
        if (deprecatedAnnotations.isEmpty())
            return false

        val kotlinDeprecated = deprecatedAnnotations.find { it.dri.packageName == "kotlin" }
        if (kotlinDeprecated?.isHidden() == true)
            return true

        return perPackageOptions(d)?.skipDeprecated ?: sourceSet(d).skipDeprecated
    }

    private fun WithExtraProperties<*>.annotations(): List<Annotations.Annotation> {
        return this.extra.allOfType<Annotations>().flatMap { annotations ->
            annotations.directAnnotations.values.singleOrNull() ?: emptyList()
        }
    }

    private fun filterDeprecatedAnnotations(annotations: List<Annotations.Annotation>): List<Annotations.Annotation> {
        return annotations.filter {
            (it.dri.packageName == "kotlin" && it.dri.classNames == "Deprecated") ||
                    (it.dri.packageName == "java.lang" && it.dri.classNames == "Deprecated")
        }
    }

    private fun Annotations.Annotation.isHidden(): Boolean {
        val level = (this.params["level"] as? EnumValue) ?: return false
        return level.enumName == "DeprecationLevel.HIDDEN"
    }
}
