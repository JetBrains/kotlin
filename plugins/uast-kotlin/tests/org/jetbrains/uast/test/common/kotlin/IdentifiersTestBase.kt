/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.test.env.kotlin.assertEqualsToFile
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.junit.ComparisonFailure
import java.io.File
import kotlin.test.fail

interface IdentifiersTestBase {
    fun getIdentifiersFile(testName: String): File

    private fun UFile.asIdentifiersWithParents() = object : IndentedPrintingVisitor(KtBlockExpression::class) {
        override fun render(element: PsiElement): CharSequence? = element.toUElementOfType<UIdentifier>()?.let { uIdentifier ->
            StringBuilder().apply {
                append(uIdentifier.sourcePsiElement!!.text)
                append(" -> ")
                append(uIdentifier.uastParent?.asLogString())
            }
        }
    }.visitUFileAndGetResult(this)

    private fun UFile.testIdentifiersParents() {
        accept(object : AbstractUastVisitor() {
            override fun visitElement(node: UElement): Boolean {
                val uIdentifier = when (node) {
                    is UAnchorOwner -> node.uastAnchor ?: return false
                    is UBinaryExpression -> node.operatorIdentifier ?: return false
                    else -> return false
                }

                assertParents(node, uIdentifier)
                val identifierSourcePsi = uIdentifier.sourcePsi ?: return false
                val operatorIdentifierFromSource = identifierSourcePsi.toUElementOfType<UIdentifier>()
                    ?: fail("$identifierSourcePsi of ${identifierSourcePsi.javaClass} should be convertable to UIdentifier")
                assertParents(node, operatorIdentifierFromSource)

                return false
            }
        })
    }

    fun assertParents(node: UElement, uastAnchor: UIdentifier?) {
        //skipping such elements because they are ambiguous (properties and getters for instance)
        if (node.sourcePsi.toUElement() != node) return
        if (uastAnchor == null)
            throw AssertionError("no uast anchor for node = $node")
        val nodeParents = node.withContainingElements.log()
        val anchorParentParents = uastAnchor.uastParent?.withContainingElements?.log() ?: ""
        // dropping node itself because we allow children to share identifiers with parents (primary constructor for instance)
        val parentsSuffix = node.withContainingElements.drop(1).log()
        if (!anchorParentParents.endsWith(parentsSuffix))
            throw ComparisonFailure(
                "wrong parents for '${uastAnchor.sourcePsi?.text}' owner: $node[${node.sourcePsi}[${node.sourcePsi?.text}]]",
                nodeParents,
                anchorParentParents
            )
    }

    private fun Sequence<UElement>.log() = this.joinToString { it.asLogString() }

    fun check(testName: String, file: UFile) {
        val valuesFile = getIdentifiersFile(testName)

        assertEqualsToFile("Identifiers", valuesFile, file.asIdentifiersWithParents())
        file.testIdentifiersParents()
    }

}
