package org.jetbrains.teamcity

import org.gradle.api.tasks.testing.AbstractTestTask
import java.io.File

object DependencyBasedTestRun {
    fun configure(task: AbstractTestTask) {
        val changedFilesPath = System.getenv(Constants.CHANGED_FILES_FILE_ENV_VAR)
        val parallelTestsPath = System.getenv(Constants.PARALLEL_TESTS_ARTIFACT_PATH_ENV_VAR)
        val riskTestsPath = System.getenv(Constants.RISK_TESTS_ARTIFACT_PATH_ENV_VAR)

        val filterWrapper = TestFilterWrapper(task.filter)

        if (!parallelTestsPath.isNullOrEmpty()) {
            filterWrapper.applyFile(File(parallelTestsPath))
        }
        if (!riskTestsPath.isNullOrEmpty()) {
            filterWrapper.applyFile(File(riskTestsPath))
        }
        if (!changedFilesPath.isNullOrEmpty()) {
            filterWrapper.applyFile(File(changedFilesPath))
        }
    }
}
