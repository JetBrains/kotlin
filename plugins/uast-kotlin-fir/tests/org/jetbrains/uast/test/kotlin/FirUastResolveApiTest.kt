/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.uast.*
import org.jetbrains.uast.test.common.kotlin.asRefNames
import org.jetbrains.uast.test.env.kotlin.AbstractFirUastTest
import org.jetbrains.uast.visitor.UastVisitor
import org.junit.Assert
import org.junit.runner.RunWith
import java.lang.IllegalStateException

@RunWith(JUnit3RunnerWithInners::class)
class FirUastResolveApiTest : AbstractFirUastTest() {
    override val isFirUastPlugin: Boolean = true

    override fun check(filePath: String, file: UFile) {
        // Bogus
    }

    // TODO: once call is supported, test labeledExpression.kt for labeled this and super

    @TestMetadata("plugins/uast-kotlin/testData")
    @TestDataPath("\$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners::class)
    class Legacy : AbstractFirUastTest() {
        override val isFirUastPlugin: Boolean = true

        override fun check(filePath: String, file: UFile) {
            // Bogus
        }

        @TestMetadata("MethodReference.kt")
        fun testMethodReference() {
            doCheck("plugins/uast-kotlin/testData/MethodReference.kt") { _, uFile ->
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
                Assert.assertTrue("Foo::bar is not a function", barReference is KtNamedFunction)
                Assert.assertEquals("Foo.bar", (barReference as KtNamedFunction).fqName?.asString())
            }
        }

        @TestMetadata("Imports.kt")
        fun testImports() {
            doCheck("plugins/uast-kotlin/testData/Imports.kt") { _, uFile ->
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
                            resolvedImport.name == "currentThread"
                        }
                        is PsiField -> {
                            // import java.lang.Thread.NORM_PRIORITY
                            resolvedImport.name == "NORM_PRIORITY"
                        }
                        is KtNamedFunction -> {
                            // import kotlin.collections.emptyList
                            resolvedImport.isTopLevel && resolvedImport.name == "emptyList"
                        }
                        is KtProperty -> {
                            // import kotlin.Int.Companion.SIZE_BYTES
                            resolvedImport.name == "SIZE_BYTES"
                        }
                        else -> false
                    }
                    Assert.assertTrue("Unexpected import: $resolvedImport", expected)
                }
            }
        }

        @TestMetadata("ReceiverFun.kt")
        fun testReceiverFun() {
            doCheck("plugins/uast-kotlin/testData/ReceiverFun.kt") { _, uFile ->
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
    }
}
