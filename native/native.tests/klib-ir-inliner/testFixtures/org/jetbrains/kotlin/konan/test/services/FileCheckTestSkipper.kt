package org.jetbrains.kotlin.konan.test.services

import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FILECHECK_STAGE
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode

/**
 * Skip all tests having `// FILECHECK_STAGE:` test directive.
 */
class FileCheckTestTotalSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        return testServices.moduleStructure.allDirectives.contains(FILECHECK_STAGE)
    }
}
