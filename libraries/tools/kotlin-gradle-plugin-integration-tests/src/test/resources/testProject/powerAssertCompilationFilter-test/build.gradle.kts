import org.jetbrains.kotlin.powerassert.gradle.PowerAssertCompilationFilter

plugins {
    kotlin("jvm")
    kotlin("plugin.power-assert")
}

dependencies {
    testImplementation(kotlin("test"))
}

powerAssert {
    functions.addAll("kotlin.require")
    compilationFilter.set(PowerAssertCompilationFilter.TESTS)
}
