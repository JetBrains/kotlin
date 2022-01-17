/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.util.AnnotationUtils
import org.junit.platform.commons.util.Preconditions

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GradleTestOsCondition(
    val enabledOn: Array<OS>
)

internal class ExecutionOnOsCondition : ExecutionCondition {
    private val isUnderTeamcity = System.getenv("TEAMCITY_VERSION") != null
    private val enabledByDefault = ConditionEvaluationResult.enabled("@GradleTestOsCondition is not present")
    private val enabledOnCurrentOs = "Enabled on operating system: " + System.getProperty("os.name")
    private val disabledOnCurrentOs = "Disabled on operating system: " + System.getProperty("os.name")

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val annotation = AnnotationUtils.findAnnotation(context.element, GradleTestOsCondition::class.java)
        if (annotation.isPresent && isUnderTeamcity) {
            val enabledOn = annotation.get().enabledOn
            Preconditions.condition(enabledOn.any(), "You must declare at least one OS in @GradleTestOsCondition")
            return if (enabledOn.any { it.isCurrentOs })
                ConditionEvaluationResult.enabled(enabledOnCurrentOs)
            else ConditionEvaluationResult.disabled(disabledOnCurrentOs)
        }

        return enabledByDefault
    }
}