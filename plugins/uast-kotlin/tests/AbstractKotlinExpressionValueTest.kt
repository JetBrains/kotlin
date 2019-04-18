/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.evaluation.uValueOf
import org.jetbrains.uast.visitor.UastVisitor

abstract class AbstractKotlinExpressionValueTest : AbstractKotlinUastTest() {

    override fun check(testName: String, file: UFile) {
        var valuesFound = 0
        file.accept(object : UastVisitor {
            override fun visitElement(node: UElement): Boolean {
                for (comment in node.comments) {
                    val text = comment.text.removePrefix("/* ").removeSuffix(" */")
                    val parts = text.split(" = ")
                    if (parts.size != 2) continue
                    when (parts[0]) {
                        "constant" -> {
                            val expectedValue = parts[1]
                            val actualValue =
                                (node as? UExpression)?.uValueOf()?.toConstant()?.toString()
                                        ?: "cannot evaluate $node of ${node.javaClass}"
                            assertEquals(expectedValue, actualValue)
                            valuesFound++
                        }
                    }
                }
                return false
            }
        })
        assertTrue("No values found, add some /* constant = ... */ to the input file", valuesFound > 0)
    }
}