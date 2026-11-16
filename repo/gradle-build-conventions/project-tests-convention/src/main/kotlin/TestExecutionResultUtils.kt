/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * The method part of a test's name as TeamCity's own Gradle runner reports it, spelled from the raw
 * `name` and `displayName` a recording keeps.
 *
 * A port of `TestNameDescriptor.DISPLAY_NAME.getTestName` from the runner's init script, see
 * gradle-runner-agent/src/main/scripts/init_since_8.gradle
 */
internal fun teamCityRunnerMethodName(name: String, displayName: String): String {
    val methodName = name.takeWhile { it !in "([{<" }
    val candidate = if (displayName.startsWith(methodName)) displayName else "$methodName($displayName)"
    return if (candidate == "$name()") name else candidate
}

/**
 * Spells a test name the way TeamCity does, `<class>.<method>`.
 *
 * [TestExecutionsListener] records the two halves apart, so that the class is not repeated in every
 * test name now that it is a suite of its own, and a replay puts them back together with this.
 *
 * A test with no class keeps the method name alone. Gradle spells "no class" two ways and both are
 * taken to mean it: null, and the empty string its JUnit Platform integration gives a test reported
 * outside any container. Joining an empty class would leave the name with a leading `.`.
 */
internal fun String?.qualifying(methodName: String): String =
    if (isNullOrEmpty()) methodName else "$this.$methodName"

/**
 * Whether Gradle inserted this suite itself - the test run, the executor, and the partitions the test
 * tasks are split into - rather than it standing for something in the test sources.
 *
 * The runner's init script drops the same ones, see `SuiteDescriptorWrapper.isIgnored`.
 */
internal fun isGradleInsertedSuiteName(suiteName: String, taskName: String): Boolean =
    suiteName.startsWith("Gradle Test Executor") ||
            suiteName.startsWith("Gradle Test Run") ||
            suiteName.startsWith("Partition") ||
            suiteName == taskName

/**
 * Whether a suite of this name carries nothing of its own in a test's TeamCity name - either Gradle
 * inserted it, or it stands for the test's own class, which TeamCity spells as part of the test name
 * rather than as an enclosing suite.
 *
 * A task may name a class suite after itself as well: Kotlin/JS reports
 * `jsNodeTest.kotlin.powerassert.DefaultMessageTest` where the JVM tasks report the bare class name.
 */
internal fun isSyntheticSuiteName(suiteName: String, taskName: String, className: String?): Boolean =
    isGradleInsertedSuiteName(suiteName, taskName) || suiteName == "$taskName.$className"
