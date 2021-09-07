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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.SystemInfoRt
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.kotlin.incremental.testingUtils.Modification

class IncrementalProjectPathCaseChangedTest : AbstractIncrementalJpsTest(checkDumpsCaseInsensitively = true) {
    fun testProjectPathCaseChanged() {
        doTest("jps-plugin/testData/incremental/custom/projectPathCaseChanged/")
    }

    fun testProjectPathCaseChangedMultiFile() {
        doTest("jps-plugin/testData/incremental/custom/projectPathCaseChangedMultiFile/")
    }

    override fun doTest(testDataPath: String) {
        if (SystemInfoRt.isFileSystemCaseSensitive) {
            return
        }

        super.doTest(testDataPath)
    }

    override fun performAdditionalModifications(modifications: List<Modification>) {
        val module = myProject.modules[0]
        val sourceRoot = module.sourceRoots[0].url
        assert(sourceRoot.endsWith("/src"))
        val newSourceRoot = sourceRoot.replace("/src", "/SRC")
        module.removeSourceRoot(sourceRoot, JavaSourceRootType.SOURCE)
        module.addSourceRoot(newSourceRoot, JavaSourceRootType.SOURCE)
    }
}
