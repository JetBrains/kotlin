/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.JavadocTag

@InternalDokkaApi
public interface DocumentationContent {
    public val tag: JavadocTag

    public fun resolveSiblings(): List<DocumentationContent>
}
