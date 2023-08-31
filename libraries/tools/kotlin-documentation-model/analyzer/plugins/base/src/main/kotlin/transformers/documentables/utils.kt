/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.ExceptionInSupertypes
import org.jetbrains.dokka.model.properties.WithExtraProperties

public val <T : Documentable> WithExtraProperties<T>.isException: Boolean
    get() = extra[ExceptionInSupertypes] != null


public val <T : Documentable> WithExtraProperties<T>.deprecatedAnnotation: Annotations.Annotation?
    get() = extra[Annotations]?.let { annotations ->
        annotations.directAnnotations.values.flatten().firstOrNull {
            it.isDeprecated()
        }
    }

/**
 * @return true if [T] has [kotlin.Deprecated] or [java.lang.Deprecated]
 *         annotation for **any** source set
 */
public fun <T : Documentable> WithExtraProperties<T>.isDeprecated(): Boolean = deprecatedAnnotation != null

/**
 * @return true for [kotlin.Deprecated] and [java.lang.Deprecated]
 */
public fun Annotations.Annotation.isDeprecated(): Boolean {
    return (this.dri.packageName == "kotlin" && this.dri.classNames == "Deprecated") ||
            (this.dri.packageName == "java.lang" && this.dri.classNames == "Deprecated")
}
