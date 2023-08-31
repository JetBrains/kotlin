/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.parsers

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.model.doc.DocumentationNode

@InternalDokkaApi
public interface DocCommentParser {
    public fun canParse(docComment: DocComment): Boolean
    public fun parse(docComment: DocComment, context: PsiNamedElement): DocumentationNode
}
