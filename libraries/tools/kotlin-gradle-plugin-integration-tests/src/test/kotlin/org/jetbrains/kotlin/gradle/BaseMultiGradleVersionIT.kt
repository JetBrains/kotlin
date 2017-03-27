package org.jetbrains.kotlin.gradle

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
open class BaseMultiGradleVersionIT : BaseGradleIT() {
    @Parameter lateinit var gradleVersion: String

    companion object {
        @JvmStatic
        @Parameters(name = "Test with Gradle version {0}")
        fun params() = listOf("2.10", "2.14.1", "3.2", "3.3", "3.4").map { arrayOf(it) }
    }
}