/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin

import com.intellij.psi.PsiClass
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.env.kotlin.AbstractFirUastTest
import org.junit.Assert
import org.junit.runner.RunWith
import java.lang.IllegalStateException

@RunWith(JUnit3RunnerWithInners::class)
class FirUastResolveApiTest : AbstractFirUastTest() {
    override val isFirUastPlugin: Boolean = true

    override fun check(filePath: String, file: UFile) {
        // Bogus
    }

    @TestMetadata("plugins/uast-kotlin-fir/testData/legacy")
    @TestDataPath("\$PROJECT_ROOT")
    @RunWith(JUnit3RunnerWithInners::class)
    class Legacy : AbstractFirUastTest() {
        override val isFirUastPlugin: Boolean = true

        override fun check(filePath: String, file: UFile) {
            // Bogus
        }

        @TestMetadata("Imports.kt")
        fun testImports() {
            doCheck("plugins/uast-kotlin-fir/testData/legacy/Imports.kt") { _, uFile ->
                uFile.imports.forEach { uImport ->
                    val resolvedImport = uImport.resolve()
                        ?: throw IllegalStateException("Unresolved import: ${uImport.asRenderString()}")
                    val expected = when (resolvedImport) {
                        is PsiClass -> {
                            // import java.lang.Thread.*
                            resolvedImport.name == "Thread"
                        }
                        is KtNamedFunction -> {
                            // import kotlin.collections.emptyList
                            resolvedImport.isTopLevel && resolvedImport.name == "emptyList"
                        }
                        is KtObjectDeclaration -> {
                            // import kotlin.Int.Companion.SIZE_BYTES
                            val selector = (uImport.importReference?.sourcePsi as? KtExpression)?.getQualifiedElementSelector()
                            resolvedImport.isCompanion() && selector?.text == "SIZE_BYTES"
                        }
                        else -> false
                    }
                    Assert.assertTrue("Unexpected import: $resolvedImport", expected)
                }
            }
        }
    }
}
