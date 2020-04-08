/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import org.jetbrains.kotlin.checkers.AbstractPsiCheckerTest
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtCodeFragment
import kotlin.test.assertNull

abstract class AbstractCodeFragmentAutoImportTest : AbstractPsiCheckerTest() {
    override fun doTest(filePath: String) {
        myFixture.configureByCodeFragment(filePath)
        myFixture.doHighlighting()

        val importFix = myFixture.availableIntentions.singleOrNull { it.familyName == "Import" }
            ?: error("No import fix available")
        importFix.invoke(project, editor, file)

        myFixture.checkResultByFile("${fileName()}.after")

        val fragment = myFixture.file as KtCodeFragment
        fragment.checkImports(testPath())

        val fixAfter = myFixture.availableIntentions.firstOrNull { it.familyName == "Import" }
        assertNull(fixAfter, "No import fix should be available after")
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}
