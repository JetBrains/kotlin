package org.jetbrains.kotlin.konan.test.services

import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FILECHECK_STAGE
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode

/**
 * When no OPT mode specified, execution of the current test having `// FILECHECK_STAGE: OptimizeTLSDataLoads` is skipped.
 */
class FileCheckTestSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        return testServices.testRunSettings.get<OptimizationMode>() != OptimizationMode.OPT &&
                testServices.moduleStructure.allDirectives[FILECHECK_STAGE].contains("OptimizeTLSDataLoads")
    }
}
