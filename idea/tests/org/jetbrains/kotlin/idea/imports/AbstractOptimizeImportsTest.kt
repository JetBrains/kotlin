/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.imports

import org.jetbrains.kotlin.AbstractImportsTest
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor
import org.jetbrains.kotlin.psi.KtFile

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
    override fun getProjectDescriptor() = KotlinStdJSProjectDescriptor.instance
}
