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

import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.kotlin.incremental.testingUtils.Modification
import org.jetbrains.kotlin.incremental.testingUtils.ModifyContent
import org.jetbrains.kotlin.jps.incremental.CacheVersionProvider

abstract class AbstractIncrementalCacheVersionChangedTest : AbstractIncrementalJpsTest(allowNoFilesWithSuffixInTestData = true) {
    override fun performAdditionalModifications(modifications: List<Modification>) {
        val modifiedFiles = modifications.filterIsInstance<ModifyContent>().map { it.path }
        val paths = projectDescriptor.dataManager.dataPaths
        val targets = projectDescriptor.allModuleTargets
        val hasKotlin = HasKotlinMarker(projectDescriptor.dataManager)

        if (modifiedFiles.any { it.endsWith("clear-has-kotlin") }) {
            targets.forEach { hasKotlin.clean(it) }
        }

        if (modifiedFiles.none { it.endsWith("do-not-change-cache-versions") }) {
            val cacheVersionProvider = CacheVersionProvider(paths)
            val versions = getVersions(cacheVersionProvider, targets)
            val versionFiles = versions.map { it.formatVersionFile }.filter { it.exists() }
            versionFiles.forEach { it.writeText("777") }
        }
    }

    protected open fun getVersions(cacheVersionProvider: CacheVersionProvider, targets: Iterable<ModuleBuildTarget>) =
            targets.map { cacheVersionProvider.normalVersion(it) }
}
