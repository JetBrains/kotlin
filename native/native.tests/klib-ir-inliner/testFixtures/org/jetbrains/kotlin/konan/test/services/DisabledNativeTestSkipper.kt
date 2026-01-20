package org.jetbrains.kotlin.konan.test.services

import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.konan.test.blackbox.support.group.isDisabledNative

/**
 * Skips execution of the current test if conditions of [org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.DISABLE_NATIVE]
 *   test directive do match the current test mode
 */
class DisabledNativeTestSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean =
        testServices.testRunSettings.isDisabledNative(testServices.moduleStructure.allDirectives)
}

