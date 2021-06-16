/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UThisExpression
import org.jetbrains.uast.test.kotlin.findFacade
import org.jetbrains.uast.visitor.UastVisitor
import org.junit.Assert
import java.lang.IllegalStateException

interface UastResolveApiTestBase : FirUastPluginSelection {
    fun checkCallbackForMethodReference(filePath: String, uFile: UFile) {
        val facade = uFile.findFacade()
            ?: throw IllegalStateException("No facade found at ${uFile.asRefNames()}")
        // val x = Foo::bar
        val x = facade.fields.single()
        var barReference: PsiElement? = null
        x.accept(object : UastVisitor {
            override fun visitElement(node: UElement): Boolean {
                return false
            }

            override fun visitCallableReferenceExpression(node: UCallableReferenceExpression): Boolean {
                barReference = node.resolve()
                return false
            }
        })
        Assert.assertNotNull("Foo::bar is not resolved", barReference)
        if (!isFirUastPlugin) {
            // TODO: FIR UAST doesn't need this unwrapping. Is this a breaking change?
            barReference = (barReference as KtLightMethod).kotlinOrigin
        }
        Assert.assertTrue("Foo::bar is not a function", barReference is KtNamedFunction)
        Assert.assertEquals("Foo.bar", (barReference as KtNamedFunction).fqName?.asString())
    }

    fun checkCallbackForImports(filePath: String, uFile: UFile) {
        uFile.imports.forEach { uImport ->
            if ((uImport.sourcePsi as? KtImportDirective)?.text?.endsWith("sleep") == true) {
                // There are two static [sleep] in [java.lang.Thread], so the import (w/o knowing its usage) can't be resolved to
                // a single function, hence `null` (as [resolve] result).
                // TODO: make [UImportStatement] a subtype of [UMultiResolvable], instead of [UResolvable]?
                return@forEach
            }
            val resolvedImport = uImport.resolve()
                ?: throw IllegalStateException("Unresolved import: ${uImport.asRenderString()}")
            val expected = when (resolvedImport) {
                is PsiClass -> {
                    // import java.lang.Thread.*
                    resolvedImport.name == "Thread" || resolvedImport.name == "UncaughtExceptionHandler"
                }
                is PsiMethod -> {
                    // import java.lang.Thread.currentThread
                    resolvedImport.name == "currentThread" ||
                            // import kotlin.collections.emptyList
                            (!isFirUastPlugin && resolvedImport.name == "emptyList")
                }
                is PsiField -> {
                    // import java.lang.Thread.NORM_PRIORITY
                    resolvedImport.name == "NORM_PRIORITY" ||
                            // import kotlin.Int.Companion.SIZE_BYTES
                            (!isFirUastPlugin && resolvedImport.name == "SIZE_BYTES")
                }
                is KtNamedFunction -> {
                    // import kotlin.collections.emptyList
                    isFirUastPlugin && resolvedImport.isTopLevel && resolvedImport.name == "emptyList"
                }
                is KtProperty -> {
                    // import kotlin.Int.Companion.SIZE_BYTES
                    isFirUastPlugin && resolvedImport.name == "SIZE_BYTES"
                }
                else -> false
            }
            Assert.assertTrue("Unexpected import: $resolvedImport", expected)
        }
    }

    fun checkCallbackForReceiverFun(filePath: String, uFile: UFile) {
        val facade = uFile.findFacade()
            ?: throw IllegalStateException("No facade found at ${uFile.asRefNames()}")
        // ... String.foo() = this.length
        val foo = facade.methods.find { it.name == "foo" }
            ?: throw IllegalStateException("Target function not found at ${uFile.asRefNames()}")
        var thisReference: PsiElement? = foo
        foo.accept(object : UastVisitor {
            override fun visitElement(node: UElement): Boolean {
                return false
            }

            override fun visitThisExpression(node: UThisExpression): Boolean {
                thisReference = node.resolve()
                return false
            }
        })
        Assert.assertNull("plain `this` has `null` label", thisReference)
    }
}
