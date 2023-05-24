/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.konan.target.Xcode
import org.jetbrains.kotlin.konan.target.XcodeVersion
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.logging.LoggerFactory

/**
 * Represents a custom annotation that specifies the minimum required XCode version for executing annotated code.
 * NOTE: If the target platform is not Mac OS this annotation will be ignored.
 *
 * This annotation can be applied to functions, annotation classes, and classes.
 *
 * @param minSupportedMajor min supported XCode version's major part. XCode versions pattern: <majorVersion>.<minorVersion>
 * @param minSupportedMinor min supported XCode version's major part. XCode versions pattern: <majorVersion>.<minorVersion>
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(RequiredXCodeExecutionCondition::class)
annotation class RequiredXCodeVersion(
    val minSupportedMajor: Int,
    val minSupportedMinor: Int,
)

/**
 * An implementation of the ExecutionCondition interface that checks if the test should be executed
 * based on the current XCode version.
 */
internal class RequiredXCodeExecutionCondition : ExecutionCondition {

    private val logger = LoggerFactory.getLogger(ExecutionOnOsCondition::class.java)

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val annotation = findAnnotation<RequiredXCodeVersion>(context)

        return if (HostManager.hostIsMac) {
            val minSupportedVersion = XcodeVersion(annotation.minSupportedMajor, annotation.minSupportedMinor)
            val currentVersion = Xcode.findCurrent().version
             if (currentVersion < minSupportedVersion) {
                logger.info { generateUnsupportedVersionMessage(minSupportedVersion, currentVersion) }
                ConditionEvaluationResult.disabled(
                    generateUnsupportedVersionMessage(minSupportedVersion, currentVersion)
                )
            } else {
                ConditionEvaluationResult.enabled(enabledOnCurrentVersionMessage(currentVersion))
            }
        } else {
            ConditionEvaluationResult.enabled("Test enabled because target OS is non MAC: ${System.getProperty("os.name")}")
        }
    }

    private fun enabledOnCurrentVersionMessage(currentVersion: Any?) =
        "Enabled on XCode version: $currentVersion"

    private fun generateUnsupportedVersionMessage(minVersion: Any?, currentVersion: Any?) =
        "Test is not supported with XCode older than $minVersion, but current is $currentVersion"

}
