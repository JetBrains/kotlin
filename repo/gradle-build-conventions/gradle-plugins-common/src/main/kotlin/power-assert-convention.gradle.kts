 import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.powerassert.gradle.PowerAssertCompilationFilter

plugins {
    kotlin("plugin.power-assert")
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
powerAssert {
    compilationFilter = PowerAssertCompilationFilter.ALL
    functions = listOf(
        "kotlin.assert",
        "kotlin.require",
        "kotlin.check",
    )
}
