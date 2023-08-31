/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
public class DocCommentFactory(
    private val docCommentCreators: List<DocCommentCreator>
) {
    public fun fromElement(element: PsiNamedElement): DocComment? {
        docCommentCreators.forEach { creator ->
            val comment = creator.create(element)
            if (comment != null) {
                return comment
            }
        }
        return null
    }
}

