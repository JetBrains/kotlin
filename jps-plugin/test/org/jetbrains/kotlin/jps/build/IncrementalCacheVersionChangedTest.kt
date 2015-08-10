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

import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import java.io.File
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import kotlin.test.assertTrue
import org.jetbrains.kotlin.jps.incremental.CacheFormatVersion

public class IncrementalCacheVersionChangedTest : AbstractIncrementalJpsTest(allowNoFilesWithSuffixInTestData = true) {
    fun testCacheVersionChanged() {
        doTest("jps-plugin/testData/incremental/custom/cacheVersionChanged/")
    }

    fun testCacheVersionChangedAndFileModified() {
        doTest("jps-plugin/testData/incremental/custom/cacheVersionChangedAndFileModified/")
    }

    fun testCacheVersionChangedMultiModule() {
        doTest("jps-plugin/testData/incremental/custom/cacheVersionChangedMultiModule/")
    }

    override fun performAdditionalModifications() {
        val storageForTargetType = BuildDataPathsImpl(myDataStorageRoot).getTargetTypeDataRoot(JavaModuleBuildTargetType.PRODUCTION)


        val moduleNames = if (getTestName(false) == "CacheVersionChangedMultiModule") listOf("module1", "module2") else listOf("module")

        for (moduleName in moduleNames) {
            val relativePath = "$moduleName/${CacheFormatVersion.FORMAT_VERSION_FILE_PATH}"
            val cacheVersionFile = File(storageForTargetType, relativePath)

            assertTrue(cacheVersionFile.exists())
            cacheVersionFile.writeText("777")
        }
    }
}
