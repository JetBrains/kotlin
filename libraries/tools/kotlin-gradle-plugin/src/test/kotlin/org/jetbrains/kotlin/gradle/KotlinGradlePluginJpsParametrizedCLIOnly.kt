/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

// Test does not follow maven failsafe naming convention
// so it is ignored by maven integration-test phase,
// but it is possible run it from CLI (under separate CI configuration for example)
//
// To run this test from CLI:
// mvn integration-test -pl :kotlin-gradle-plugin -Dit.test=KotlinGradlePluginJpsParametrizedCLIOnly
@RunWith(Parameterized::class)
class KotlinGradlePluginJpsParametrizedCLIOnly : BaseIncrementalGradleIT() {

    @Parameterized.Parameter
    @JvmField
    var relativePath: String = ""

    @Test
    fun testFromJps() {
        JpsTestProject(buildLogFinder, jpsResourcesPath, relativePath).performAndAssertBuildStages(weakTesting = true)
    }

    override fun defaultBuildOptions(): BuildOptions =
            BuildOptions(withDaemon = true, incremental = true)

    companion object {

        private val jpsResourcesPath = File("../../../jps-plugin/testData/incremental")
        private val ignoredDirs = setOf(File(jpsResourcesPath, "cacheVersionChanged"),
                                        File(jpsResourcesPath, "changeIncrementalOption"),
                                        File(jpsResourcesPath, "custom"),
                                        File(jpsResourcesPath, "lookupTracker"))
        private val buildLogFinder = BuildLogFinder(isExperimentalEnabled = true, isGradleEnabled = true)

        @Suppress("unused")
        @Parameterized.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): List<Array<String>> =
                jpsResourcesPath.walk()
                        .onEnter { it !in ignoredDirs }
                        .filter { it.isDirectory && buildLogFinder.findBuildLog(it) != null  }
                        .map { arrayOf(it.toRelativeString(jpsResourcesPath)) }
                        .toList()
    }
}

