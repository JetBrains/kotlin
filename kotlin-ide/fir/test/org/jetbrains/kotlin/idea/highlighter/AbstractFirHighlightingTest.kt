/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractFirHighlightingTest : AbstractHighlightingTest() {
    override fun getDefaultProjectDescriptor() = ProjectDescriptorWithStdlibSources.INSTANCE

    override fun checkHighlighting(fileText: String) {
        val checkInfos = !InTextDirectivesUtils.isDirectiveDefined(fileText, NO_CHECK_INFOS_PREFIX);

        // warnings are not supported yet
        myFixture.checkHighlighting(/* checkWarnings= */ false, checkInfos, /* checkWeakWarnings= */ false)
    }
}