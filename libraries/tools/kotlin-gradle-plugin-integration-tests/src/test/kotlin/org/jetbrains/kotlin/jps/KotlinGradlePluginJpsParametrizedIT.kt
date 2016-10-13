package org.jetbrains.kotlin.jps

import org.jetbrains.kotlin.incremental.testingUtils.BuildLogFinder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
@RunWith(Parameterized::class)
class KotlinGradlePluginJpsParametrizedIT : BaseIncrementalGradleIT() {

    @Parameterized.Parameter
    @JvmField
    var relativePath: String = ""

    @Test
    fun testFromJps() {
        JpsTestProject(buildLogFinder, jpsResourcesPath, relativePath).performAndAssertBuildStages(weakTesting = true)
    }

    override fun defaultBuildOptions() =
            super.defaultBuildOptions().copy(incremental = true)

    companion object {

        private val jpsResourcesPath = File("../../../jps-plugin/testData/incremental")
        private val ignoredDirs = setOf(File(jpsResourcesPath, "cacheVersionChanged"),
                                        File(jpsResourcesPath, "changeIncrementalOption"),
                                        File(jpsResourcesPath, "custom"),
                                        File(jpsResourcesPath, "lookupTracker"))
        private val buildLogFinder = BuildLogFinder(isExperimentalEnabled = true, isGradleEnabled = true)

        @Suppress("unused")
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun data(): List<Array<String>> =
                jpsResourcesPath.walk()
                        .onEnter { it !in ignoredDirs }
                        .filter { it.isDirectory && buildLogFinder.findBuildLog(it) != null  }
                        .map { arrayOf(it.toRelativeString(jpsResourcesPath)) }
                        .toList()
    }
}

