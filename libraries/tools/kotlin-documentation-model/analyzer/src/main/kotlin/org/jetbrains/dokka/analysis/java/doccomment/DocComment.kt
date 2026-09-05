/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.JavadocTag

/**
 * MUST override equals and hashcode
 */
@InternalDokkaApi
public interface DocComment {
    public fun hasTag(tag: JavadocTag): Boolean

    public fun resolveTag(tag: JavadocTag): List<DocumentationContent>
}
