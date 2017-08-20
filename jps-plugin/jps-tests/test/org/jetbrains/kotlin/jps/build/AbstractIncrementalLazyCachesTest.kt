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
import org.jetbrains.jps.builders.BuildTarget
import org.jetbrains.jps.builders.storage.BuildDataPaths
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.incremental.CacheVersion
import org.jetbrains.kotlin.incremental.KOTLIN_CACHE_DIRECTORY_NAME
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.testingUtils.Modification
import org.jetbrains.kotlin.incremental.testingUtils.ModifyContent
import org.jetbrains.kotlin.jps.incremental.CacheVersionProvider
import org.jetbrains.kotlin.jps.incremental.KotlinDataContainerTarget
import org.jetbrains.kotlin.utils.Printer
import java.io.File

abstract class AbstractIncrementalLazyCachesTest : AbstractIncrementalJpsTest() {
    private val expectedCachesFileName: String
        get() = "expected-kotlin-caches.txt"

    override fun doTest(testDataPath: String) {
        super.doTest(testDataPath)

        val actual = dumpKotlinCachesFileNames()
        val expectedFile = File(testDataPath, expectedCachesFileName)
        UsefulTestCase.assertSameLinesWithFile(expectedFile.canonicalPath, actual)
    }

    override fun performAdditionalModifications(modifications: List<Modification>) {
        super.performAdditionalModifications(modifications)

        for (modification in modifications) {
            if (modification !is ModifyContent) continue

            val name = File(modification.path).name

            when {
                name.endsWith("incremental-compilation") -> {
                    IncrementalCompilation.setIsEnabled(modification.dataFile.readAsBool())
                }
            }
        }
    }

    fun File.readAsBool(): Boolean {
        val content = this.readText()

        return when (content.trim()) {
            "on" -> true
            "off" -> false
            else -> throw IllegalStateException("$this content is expected to be 'on' or 'off'")
        }
    }

    private fun dumpKotlinCachesFileNames(): String {
        val sb = StringBuilder()
        val p = Printer(sb)
        val targets = projectDescriptor.allModuleTargets
        val dataManager = projectDescriptor.dataManager
        val paths = dataManager.dataPaths
        val versions = CacheVersionProvider(paths)

        dumpCachesForTarget(p, paths, KotlinDataContainerTarget, versions.dataContainerVersion().formatVersionFile)

        for (target in targets.sortedBy { it.presentableName }) {
            val jvmMetaBuildInfo = jvmBuildMetaInfoFile(target, dataManager)
            dumpCachesForTarget(p, paths, target,
                                versions.normalVersion(target).formatVersionFile,
                                jvmMetaBuildInfo,
                                subdirectory = KOTLIN_CACHE_DIRECTORY_NAME)
        }

        return sb.toString()
    }

    private fun dumpCachesForTarget(
            p: Printer,
            paths: BuildDataPaths,
            target: BuildTarget<*>,
            vararg cacheVersionsFiles: File,
            subdirectory: String? = null
    ) {
        p.println(target)
        p.pushIndent()

        val dataRoot = paths.getTargetDataRoot(target).let { if (subdirectory != null) File(it, subdirectory) else it }
        cacheVersionsFiles
                .filter(File::exists)
                .sortedBy { it.name }
                .forEach { p.println(it.name) }

        kotlinCacheNames(dataRoot).sorted().forEach { p.println(it) }

        p.popIndent()
    }

    private fun kotlinCacheNames(dir: File): List<String> {
        val result = arrayListOf<String>()

        for (file in dir.walk()) {
            if (file.isFile && file.extension == BasicMapsOwner.CACHE_EXTENSION) {
                result.add(file.name)
            }
        }

        return result
    }
}
