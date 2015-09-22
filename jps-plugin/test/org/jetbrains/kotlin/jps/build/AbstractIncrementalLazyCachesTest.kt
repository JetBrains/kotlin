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

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.jps.incremental.IncrementalCacheImpl
import org.jetbrains.kotlin.jps.incremental.getCacheDirectoryName
import org.jetbrains.kotlin.utils.Printer
import java.io.File

public abstract class AbstractIncrementalLazyCachesTest : AbstractIncrementalJpsTest() {
    override fun doTest(testDataPath: String) {
        super.doTest(testDataPath)

        val actual = dumpKotlinCachesFileNames()
        val expectedFile = File(testDataPath, "expected-kotlin-caches.txt")
        UsefulTestCase.assertSameLinesWithFile(expectedFile.canonicalPath, actual)
    }

    private fun dumpKotlinCachesFileNames(): String {
        val sb = StringBuilder()
        val p = Printer(sb)
        val targets = projectDescriptor.allModuleTargets
        val paths = projectDescriptor.dataManager.dataPaths

        for (target in targets.sortedBy { it.presentableName }) {
            p.println(target)
            p.pushIndent()

            val dataRoot = paths.getTargetDataRoot(target)
            val cacheNames = kotlinCacheNames(dataRoot)
            cacheNames.sorted().forEach { p.println(it) }

            p.popIndent()
        }

        return sb.toString()
    }

    private fun kotlinCacheNames(dataRoot: File): List<String> {
        val cacheDir = File(dataRoot, getCacheDirectoryName())
        val fileNames = cacheDir.list() ?: arrayOf()
        val cacheFiles = fileNames
                .map { File(cacheDir, it) }
                .filter { it.isFile && it.extension == IncrementalCacheImpl.CACHE_EXTENSION }
        return cacheFiles.map { it.name }
    }
}