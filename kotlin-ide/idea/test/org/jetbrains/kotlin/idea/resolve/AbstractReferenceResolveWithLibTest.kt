/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.resolve

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.impl.PsiDelegateReference
import org.jetbrains.kotlin.idea.test.AstAccessControl.ALLOW_AST_ACCESS_DIRECTIVE
import org.jetbrains.kotlin.idea.test.AstAccessControl.execute
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractReferenceResolveWithLibTest : AbstractReferenceResolveTest() {
    private companion object {
        val MOCK_SOURCES_BASE = IDEA_TEST_DATA_DIR.resolve("resolve/referenceWithLib")
    }

    private lateinit var mockLibraryFacility: MockLibraryFacility

    override fun setUp() {
        super.setUp()
        mockLibraryFacility = MockLibraryFacility(File(MOCK_SOURCES_BASE, getTestName(true) + "Src"), attachSources = false)
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() {
        mockLibraryFacility.tearDown(module)
        super.tearDown()
    }

    override fun wrapReference(reference: PsiReference?): PsiReference? {
        if (reference == null) {
            return null
        } else if (InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, ALLOW_AST_ACCESS_DIRECTIVE)) {
            return reference
        }

        return object : PsiDelegateReference(reference) {
            override fun resolve(): PsiElement? {
                return execute(false, testRootDisposable, myFixture) {
                    reference.resolve() ?: error("Reference can't be resolved")
                }
            }

            override fun toString(): String {
                return reference.toString()
            }
        }
    }
}