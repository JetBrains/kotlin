package org.jetbrains.kotlin.gradle

import org.junit.Ignore
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
        JpsTestProject(jpsResourcesPath, relativePath).performAndAssertBuildStages(weakTesting = true)
    }

    override fun defaultBuildOptions(): BuildOptions = BuildOptions(withDaemon = true)

    companion object {

        private val jpsResourcesPath = File("../../../jps-plugin/testData/incremental")
        private val ignoredDirs = setOf(File(jpsResourcesPath, "cacheVersionChanged"),
                                        File(jpsResourcesPath, "changeIncrementalOption"),
                                        File(jpsResourcesPath, "custom"),
                                        File(jpsResourcesPath, "lookupTracker"))

        @Suppress("unused")
        @Parameterized.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): List<Array<String>> =
                jpsResourcesPath.walk()
                        .onEnter { it !in ignoredDirs }
                        .filter { it.isDirectory && isJpsTestProject(it) }
                        .map { arrayOf(it.toRelativeString(jpsResourcesPath)) }
                        .toList()
    }
}

