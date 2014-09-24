/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.jps.build

import com.intellij.testFramework.TestDataPath
import org.jetbrains.jet.JetTestUtils
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl
import java.io.File
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jet.jps.incremental
import com.sun.tools.javac.resources.version
import org.jetbrains.jet.jps.incremental.IncrementalCacheImpl
import kotlin.test.assertTrue

public class IncrementalCacheVersionChangedTest : AbstractIncrementalJpsTest() {
    fun testCacheVersionChanged() {
        doTest("jps-plugin/testData/incremental/custom/cacheVersionChanged/")
    }

    fun testCacheVersionChangedAndFileModified() {
        doTest("jps-plugin/testData/incremental/custom/cacheVersionChangedAndFileModified/")
    }

    override val customTest: Boolean
        get() = true

    override fun performAdditionalModifications() {
        val storageForTargetType = BuildDataPathsImpl(myDataStorageRoot).getTargetTypeDataRoot(JavaModuleBuildTargetType.PRODUCTION)
        val relativePath = "module/${File.separator}${IncrementalCacheImpl.DIRECTORY_NAME}/${IncrementalCacheImpl.FORMAT_VERSION_TXT}"
        val cacheVersionFile = File(storageForTargetType, relativePath)

        assertTrue(cacheVersionFile.exists())
        cacheVersionFile.writeText("777")
    }
}