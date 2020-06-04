/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import org.jetbrains.kotlin.idea.navigation.GotoCheck
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.junit.Assert

abstract class AbstractScriptConfigurationNavigationTest : AbstractScriptConfigurationTest() {

    fun doTest(unused: String) {
        configureScriptFile(testDataFile())
        val reference = file!!.findReferenceAt(myEditor.caretModel.offset)!!

        val resolved = reference.resolve()!!.navigationElement!!

        val expectedReference = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// REF:")
        val actualReference = resolved.renderAsGotoImplementation()

        Assert.assertEquals(expectedReference, actualReference)

        val expectedFile = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// FILE:")
        val actualFile = GotoCheck.getFileWithDir(resolved)

        Assert.assertEquals(expectedFile, actualFile)
    }
}