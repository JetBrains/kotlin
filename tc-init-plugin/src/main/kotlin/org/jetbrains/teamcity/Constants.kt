package org.jetbrains.teamcity

object Constants {
    // Environment variable which points to the TeamCity build properties file. Is set by the agent's core logic.
    const val BUILD_PROPERTIES_FILE_ENV_VAR = "TEAMCITY_BUILD_PROPERTIES_FILE"

    // region Environment variables which are set in the Gradle runner's agent logic and are used internally by the runner's init scripts.

    // Points to the TeamCity build temporary directory. Is set based on the system property provided by the agent's core logic.
    const val BUILD_TEMP_DIR_ENV_VAR = "TEAMCITY_BUILD_TEMP_DIR"
    // Points to the file that lists changed files for dependency-based test runs. Is set based on the system property provided by the agent's core logic.
    const val CHANGED_FILES_FILE_ENV_VAR = "TEAMCITY_CHANGED_FILES_FILE"
    // Points to the artifact with tests selected for parallel execution. Is set based on the system property provided by the agent's core logic.
    const val PARALLEL_TESTS_ARTIFACT_PATH_ENV_VAR = "TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH"
    // Points to the artifact with risk-based tests selected for execution. Is set based on the system property provided by the agent's core logic.
    const val RISK_TESTS_ARTIFACT_PATH_ENV_VAR = "TEAMCITY_RISK_TESTS_ARTIFACT_PATH"
    // Defines the incremental building mode. Is set based on the build step configuration.
    const val GRADLE_INCREMENTAL_MODE_ENV_VAR = "TEAMCITY_GRADLE_INCREMENTAL_MODE"
    // Indicates whether test retry is supported on the build configuration level. Is set based on the build configuration's test retry setup.
    const val SUPPORT_TEST_RETRY_ENV_VAR = "TEAMCITY_SUPPORT_TEST_RETRY"
    // Lists classes to add from test classpath into Gradle daemon classloader. Is defined by the Gradle runner's agent logic.
    const val GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_VAR = "TEAMCITY_GRADLE_DAEMON_ENHANCEMENT_CLASSES"
    // Supplies extra JVM arguments for Gradle test tasks. Is set by the agent's test coverage logic.
    const val GRADLE_TEST_COVERAGE_JVM_ARGS_ENV_VAR = "TEAMCITY_GRADLE_TEST_COVERAGE_JVM_ARGS"
    // Disables adding TeamCity build properties to Gradle project extra properties. Is set from the configuration parameters.
    const val DO_NOT_POPULATE_GRADLE_PROPERTIES_ENV_VAR = "TEAMCITY_DO_NOT_POPULATE_GRADLE_PROPERTIES"
    // Controls whether explicit test retry integration should inspect Gradle retry plugins. Is set from the configuration parameters.
    const val USE_TEST_RETRY_PLUGIN_ENV_VAR = "TEAMCITY_USE_TEST_RETRY_PLUGIN"
    // Disables adding TeamCity test JVM arguments through a command line argument provider. Is set from the configuration parameters.
    const val TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_VAR = "TEAMCITY_TEST_TASK_JVM_ARGUMENTS_PROVIDER_DISABLED"
    // Selects the format used for TeamCity test names. Is set from the configuration parameters.
    const val TEST_NAME_FORMAT_ENV_VAR = "TEAMCITY_TEST_NAME_FORMAT"
    // Selects the format used for ignored TeamCity test suites. Is set from the configuration parameters.
    const val IGNORED_SUITE_FORMAT_ENV_VAR = "TEAMCITY_IGNORE_SUITE_FORMAT_FORMAT"
    // Points to the directory where TeamCity stores stacktrace logs. Is set based on a system property, the origin of which is unknown. Left for backwards compatibility for now.
    const val STACKTRACE_LOG_DIR_ENV_VAR = "TEAMCITY_STACKTRACE_LOG_DIR"

    // endregion

    // region System properties not documented or used anywhere outside the runner's init scripts. Left for backwards compatibility.

    // Selects the parser used for TeamCity service messages in Gradle output.
    const val MESSAGE_PARSER_TYPE_SYSTEM_PROP = "teamcity.gradle.message.parser.type"
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

    // endregion

    // Limits the size of internal String buffer that captures a compile task's stderr
    const val COMPILE_ERROR_MAX_LENGTH_SYSTEM_PROP = "teamcity.gradle.compileError.maxLength"

    // Gradle's isolated projects system property flag for Gradle versions where isolated projects is an incubating feature.
    const val GRADLE_UNSAFE_ISOLATED_PROJECTS_SYSTEM_PROP = "org.gradle.unsafe.isolated-projects"
}
