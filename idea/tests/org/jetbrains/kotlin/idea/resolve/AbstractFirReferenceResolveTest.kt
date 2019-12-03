/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.resolve

import org.jetbrains.kotlin.idea.completion.test.configureWithExtraFile
import org.jetbrains.kotlin.idea.fir.FirResolution
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractFirReferenceResolveTest : AbstractReferenceResolveTest() {
    override fun setUp() {
        super.setUp()
        FirResolution.enabled = true
    }

    override fun doTest(path: String) {
        assert(path.endsWith(".kt")) { path }
        myFixture.configureWithExtraFile(path, ".Data")
        if (InTextDirectivesUtils.isDirectiveDefined(myFixture.file.text, "IGNORE_FIR")) {
            return
        }
        performChecks()
    }

    override fun tearDown() {
        FirResolution.enabled = false
        super.tearDown()
    }
}