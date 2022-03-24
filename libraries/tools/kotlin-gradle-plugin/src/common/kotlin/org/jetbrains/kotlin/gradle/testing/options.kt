package org.jetbrains.kotlin.gradle.testing

import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinTestRunnerCliArgs

@Suppress("EnumEntryName")
enum class IgnoredTestSuites(val cli: KotlinTestRunnerCliArgs.IgnoredTestSuitesReporting) {
    hide(KotlinTestRunnerCliArgs.IgnoredTestSuitesReporting.skip),
    showWithContents(KotlinTestRunnerCliArgs.IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored),
    showWithoutContents(KotlinTestRunnerCliArgs.IgnoredTestSuitesReporting.reportAsIgnoredTest)
}

@Suppress("EnumEntryName")
enum class TestsGrouping {
    none,
    root,
    leaf;
}

