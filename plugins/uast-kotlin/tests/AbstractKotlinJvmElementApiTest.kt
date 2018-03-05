/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("AbstractKotlinRenderLogTestKt")

package org.jetbrains.uast.test.kotlin

import com.intellij.lang.jvm.JvmElement
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.uast.UAnchorOwner
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.common.RenderLogTestBase
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File
import kotlin.test.assertTrue

abstract class AbstractKotlinJvmElementApiTest : AbstractKotlinUastTest(), RenderLogTestBase {
    override fun getTestFile(testName: String, ext: String) =
        File(File(TEST_KOTLIN_MODEL_DIR, testName).canonicalPath + '.' + ext)

    override fun check(testName: String, file: UFile) {
        (file.sourcePsi as KtFile).checkContainingFileForAllElements()
        file.checkUElementJvmPsiRepresentations()
    }


    private fun KtFile.checkContainingFileForAllElements() {

        fun checkLightElementAsJvmElements(psi: PsiElement) {
            (psi as? JvmElement)?.let { checkJvmElement(psi, null) }
            for (child in psi.children) {
                checkLightElementAsJvmElements(child)
            }
        }

        val classes = this.toLightElements()
        println("classes = " + classes.joinToString())
        classes.forEach(::checkLightElementAsJvmElements)
    }

    private fun UFile.checkUElementJvmPsiRepresentations() {
        accept(object : UastVisitor {
            override fun visitElement(node: UElement): Boolean {

                if (node is UAnchorOwner) {
                    node.uastAnchor?.accept(this)
                }

                val javaRepresentation = node.javaPsi ?: return false
//                val jvmElement = javaRepresentation.assertedCast<JvmElement> { "java representation of $node should be JvmElement" }
                val jvmElement = javaRepresentation as? JvmElement ?: return false
                checkJvmElement(jvmElement, node)

                return false
            }
        })
    }

    private fun checkJvmElement(jvmElement: JvmElement, comesFrom: Any?) {
        println("checkJvmElement $jvmElement")
        val sourceElement = try {
            jvmElement.sourceElement ?: return
        } catch (e: IllegalStateException) {
            throw AssertionError("cant get `sourceElement` for $jvmElement of ${jvmElement.javaClass}"
                                         + (comesFrom?.let { " comes from $it of (${it.javaClass})" } ?: ""), e)
        }
        assertTrue(sourceElement.isPhysical, "$sourceElement of ${sourceElement.javaClass} should be physical")
        assertTrue(sourceElement is KtElement, "$sourceElement of ${sourceElement.javaClass} should be KtElement")

        println("checkUElementJvmPsiRepresentations $sourceElement")
    }
}

