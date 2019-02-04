/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.imports

import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor

abstract class AbstractOptimizeImportsTest() : AbstractImportsTest() {
    override fun doTest(file: KtFile): String {
        OptimizedImportsBuilder.testLog = StringBuilder()
        try {
            KotlinImportOptimizer().processFile(file).run()
            return OptimizedImportsBuilder.testLog.toString()
        }
        finally {
            OptimizedImportsBuilder.testLog = null
        }
    }

    override val nameCountToUseStarImportDefault: Int
        get() = Integer.MAX_VALUE
}

abstract class AbstractJvmOptimizeImportsTest : AbstractOptimizeImportsTest()

abstract class AbstractJsOptimizeImportsTest : AbstractOptimizeImportsTest() {
    override fun getProjectDescriptor() = KotlinStdJSProjectDescriptor
}
