/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.doccomment

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.util.from
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.utilities.DokkaLogger

@InternalDokkaApi
public class DocCommentFinder(
    private val logger: DokkaLogger,
    private val docCommentFactory: DocCommentFactory,
) {
    public fun findClosestToElement(element: PsiNamedElement): DocComment? {
        val docComment = docCommentFactory.fromElement(element)
        if (docComment != null) {
            return docComment
        }

        return if (element is PsiMethod) {
            findClosestToMethod(element)
        } else {
            element.children
                .filterIsInstance<PsiDocComment>()
                .firstOrNull()
                ?.let { JavaDocComment(it) }
        }
    }

    private fun findClosestToMethod(method: PsiMethod): DocComment? {
        val superMethods = method.findSuperMethods()
        if (superMethods.isEmpty()) return null

        if (superMethods.size == 1) {
            return findClosestToElement(superMethods.single())
        }

        val superMethodDocumentation = superMethods.map { superMethod -> findClosestToElement(superMethod) }.distinct()
        if (superMethodDocumentation.size == 1) {
            return superMethodDocumentation.single()
        }

        logger.debug(
            "Conflicting documentation for ${DRI.from(method)}" +
                    "${superMethods.map { DRI.from(it) }}"
        )

        /* Prioritize super class over interface */
        val indexOfSuperClass = superMethods.indexOfFirst { superMethod ->
            val parent = superMethod.parent
            if (parent is PsiClass) !parent.isInterface
            else false
        }

        return if (indexOfSuperClass >= 0) {
            superMethodDocumentation[indexOfSuperClass]
        } else {
            superMethodDocumentation.first()
        }
    }
}
