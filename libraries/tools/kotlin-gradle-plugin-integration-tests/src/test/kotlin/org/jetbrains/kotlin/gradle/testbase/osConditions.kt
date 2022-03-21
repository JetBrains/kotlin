/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(ExecutionOnOsCondition::class)
annotation class OsCondition(
    val supportedOn: Array<OS> = [OS.LINUX, OS.MAC, OS.WINDOWS],
    val enabledOnCI: Array<OS> = [OS.LINUX, OS.WINDOWS]
)

internal class ExecutionOnOsCondition : ExecutionCondition {
    private val isUnderTeamcity = System.getenv("TEAMCITY_VERSION") != null
    private val disabledByDefault = ConditionEvaluationResult.disabled("@OsCondition is not present")

    private val enabledOnCurrentOs = "Enabled on operating system: " + System.getProperty("os.name")
    private val notSupportedOnCurrentOs = "Test is not supported on operating system: " + System.getProperty("os.name")
    private val disabledForCI = "Disabled for operating system: " + System.getProperty("os.name") + " on CI"

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val annotation = AnnotationUtils.findAnnotation(context.element, OsCondition::class.java)
        if (annotation.isPresent) {
            val supportedOn = annotation.get().supportedOn
            val enabledOnCI = annotation.get().enabledOnCI

            return if (!supportedOn.any { it.isCurrentOs })
                ConditionEvaluationResult.disabled(notSupportedOnCurrentOs)
            else if (isUnderTeamcity && !enabledOnCI.any { it.isCurrentOs })
                ConditionEvaluationResult.disabled(disabledForCI)
            else ConditionEvaluationResult.enabled(enabledOnCurrentOs)
        }

        return disabledByDefault
    }
}