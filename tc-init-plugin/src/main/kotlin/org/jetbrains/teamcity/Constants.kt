package org.jetbrains.teamcity

object Constants {
    // Environment variable which points to the TeamCity build properties file.
    const val BUILD_PROPERTIES_FILE_ENV_VAR = "TEAMCITY_BUILD_PROPERTIES_FILE"

    // Points to the file that lists changed files for dependency-based test runs.
    const val CHANGED_FILES_FILE_ENV_VAR = "TEAMCITY_CHANGED_FILES_FILE"
    // Points to the artifact with tests selected for parallel execution.
    const val PARALLEL_TESTS_ARTIFACT_PATH_ENV_VAR = "TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH"
    // Points to the artifact with risk-based tests selected for execution.
    const val RISK_TESTS_ARTIFACT_PATH_ENV_VAR = "TEAMCITY_RISK_TESTS_ARTIFACT_PATH"
    // Supplies extra JVM arguments for Gradle test tasks.
    const val GRADLE_TEST_COVERAGE_JVM_ARGS_ENV_VAR = "TEAMCITY_GRADLE_TEST_COVERAGE_JVM_ARGS"
    // Disables adding TeamCity build properties to Gradle project extra properties.
    const val DO_NOT_POPULATE_GRADLE_PROPERTIES_ENV_VAR = "TEAMCITY_DO_NOT_POPULATE_GRADLE_PROPERTIES"
    // Disables adding TeamCity test JVM arguments through a command line argument provider.
    const val TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_VAR = "TEAMCITY_TEST_TASK_JVM_ARGUMENTS_PROVIDER_DISABLED"
    // Selects the format used for TeamCity test names.
    const val TEST_NAME_FORMAT_ENV_VAR = "TEAMCITY_TEST_NAME_FORMAT"
    // Selects the format used for ignored TeamCity test suites.
    const val IGNORED_SUITE_FORMAT_ENV_VAR = "TEAMCITY_IGNORE_SUITE_FORMAT_FORMAT"
    // Points to the directory where TeamCity stores stacktrace logs.
    const val STACKTRACE_LOG_DIR_ENV_VAR = "TEAMCITY_STACKTRACE_LOG_DIR"

    // Limits the size of test stdout/stderr service messages.
    const val MESSAGE_MAX_LENGTH_SYSTEM_PROP = "teamcity.gradle.message.maxLength"
    // Limits the size of stacktraces reported in test failure messages.
    const val STACKTRACE_MAX_LENGTH_SYSTEM_PROP = "teamcity.gradle.stacktrace.maxLength"
    // Controls when large test exception details are attached as artifacts instead of inline messages.
    const val MIN_ATTACHED_TEST_EXCEPTION_SYSTEM_PROP = "teamcity.gradle.minAttachedTestException"
    // Suppresses forwarding test stdout to TeamCity when enabled.
    const val IGNORE_TEST_STDOUT_SYSTEM_PROP = "teamcity.ignoreTestStdOut"
    // Suppresses forwarding test stderr to TeamCity when enabled.
    const val IGNORE_TEST_STDERR_SYSTEM_PROP = "teamcity.ignoreTestStdErr"

    // Limits the size of internal String buffer that captures a compile task's stderr
    const val COMPILE_ERROR_MAX_LENGTH_SYSTEM_PROP = "teamcity.gradle.compileError.maxLength"
}
