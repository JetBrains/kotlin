package org.jetbrains.kotlin.gradle

import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@Ignore
@RunWith(Parameterized::class)
class KotlinGradlePluginJpsParametrizedIT : BaseIncrementalGradleIT() {

    @Parameterized.Parameter
    @JvmField
    var relativePath: String = ""

    @Test
    fun testFromJps() {
        try {
            JpsTestProject(jpsResourcesPath, relativePath).performAndAssertBuildStages(weakTesting = true)
        }
        finally {
            if (defaultBuildOptions().withDaemon)
                checkRecycleDaemon()
        }
    }

    override fun defaultBuildOptions(): BuildOptions = BuildOptions(withDaemon = true)

    companion object {

        private val jpsResourcesPath = File("../../../jps-plugin/testData/incremental")

        val MAX_TESTS_ON_SINGLE_DAEMON = 20
        private var testsCounter = 0

        @Suppress("unused")
        @Parameterized.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): List<Array<String>> =
                jpsResourcesPath.walk()
                        .filter { it.isDirectory && isJpsTestProject(it) }
                        .map { arrayOf(it.toRelativeString(jpsResourcesPath)) }
                        .toList()

        @Synchronized
        fun checkRecycleDaemon() {
            if (testsCounter++ > MAX_TESTS_ON_SINGLE_DAEMON) {
                BaseGradleIT.tearDownAll()
                testsCounter = 0
            }
        }
    }
}

