package org.jetbrains.kotlin.konan.test.services

import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.test.services.MetaTestConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.util.defFileIsSupportedOn
/**
 * Skips execution of the current test if `.def` file is not supported on the current test target (for ex, Objective-C on Linux/Win)
 */
class CInteropTestSkipper(testServices: TestServices) : MetaTestConfigurator(testServices) {
    override fun shouldSkipTest(): Boolean {
        val testTarget = testServices.testRunSettings.get<KotlinNativeTargets>().testTarget
        return testServices.moduleStructure.modules.any {
            it.files.any { file ->
                file.name.endsWith(".def") &&
                        !file.originalFile.defFileIsSupportedOn(testTarget)
            }
        }
    }
}


