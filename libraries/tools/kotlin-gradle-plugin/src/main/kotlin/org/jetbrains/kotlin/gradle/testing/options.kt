package org.jetbrains.kotlin.gradle.testing

import org.jetbrains.kotlin.gradle.targets.js.tasks.KotlinNodeJsTestRunnerCliArgs

@Suppress("EnumEntryName")
enum class IgnoredTestSuites(val cli: KotlinNodeJsTestRunnerCliArgs.IgnoredTestSuitesReporting) {
    hide(KotlinNodeJsTestRunnerCliArgs.IgnoredTestSuitesReporting.skip),
    showWithContents(KotlinNodeJsTestRunnerCliArgs.IgnoredTestSuitesReporting.reportAllInnerTestsAsIgnored),
    showWithoutContents(KotlinNodeJsTestRunnerCliArgs.IgnoredTestSuitesReporting.reportAsIgnoredTest)
}

@Suppress("EnumEntryName")
enum class TestsGrouping {
    none,
    root,
    leaf;
}

