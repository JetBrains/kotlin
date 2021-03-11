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

import org.jetbrains.kotlin.incremental.testingUtils.Modification
import org.jetbrains.kotlin.incremental.testingUtils.ModifyContent
import org.jetbrains.kotlin.jps.incremental.CacheVersionManager
import org.jetbrains.kotlin.jps.targets.KotlinModuleBuildTarget

/**
 * @see [jps-plugin/testData/incremental/cacheVersionChanged/README.md]
 */
abstract class AbstractIncrementalCacheVersionChangedTest : AbstractIncrementalJvmJpsTest(allowNoFilesWithSuffixInTestData = true) {
    override fun performAdditionalModifications(modifications: List<Modification>) {
        val modifiedFiles = modifications.filterIsInstance<ModifyContent>().map { it.path }
        val targets = projectDescriptor.allModuleTargets
        val hasKotlin = HasKotlinMarker(projectDescriptor.dataManager)

        if (modifiedFiles.any { it.endsWith("clear-has-kotlin") }) {
            targets.forEach { hasKotlin.clean(it) }
        }

        if (modifiedFiles.none { it.endsWith("do-not-change-cache-versions") }) {
            val versions = targets.flatMap {
                getVersionManagersToTest(kotlinCompileContext.targetsBinding[it]!!)
            }

            versions.forEach {
                if (it.versionFileForTesting.exists()) {
                    it.versionFileForTesting.writeText("777")
                }
            }
        }
    }

    protected open fun getVersionManagersToTest(target: KotlinModuleBuildTarget<*>): List<CacheVersionManager> =
        listOf(target.localCacheVersionManager)
}
