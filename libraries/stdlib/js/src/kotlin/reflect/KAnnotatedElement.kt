/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

/**
 * Represents an annotated element and allows to obtain its annotations.
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/annotations.html)
 * for more information.
 */
public interface KAnnotatedElement {
    /**
     * Annotations which are present on this element.
     */
    public val annotations: List<Annotation>
}
