/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers.doctag

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.doccomment.DocumentationContent

@InternalDokkaApi
public interface InheritDocTagContentProvider {
    public fun canConvert(content: DocumentationContent): Boolean
    public fun convertToHtml(content: DocumentationContent, docTagParserContext: DocTagParserContext): String
}
