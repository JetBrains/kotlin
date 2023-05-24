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
import org.junit.platform.commons.logging.LoggerFactory
import java.lang.reflect.AnnotatedElement

/**
 * An annotation that enables tests to be executed on a specific operating system within a specific environment.
 *
 * @param supportedOn Declares all the systems, on which this test can be executed in general, no matter on which environment.
 * @param enabledOnCI Declares only platforms on which this test should be run in the TeamCity builds
 *                      (that helps us not to overload such platforms like Mac OS).
 *
 * ## Warning
 * If this annotation is directly present, indirectly present, or meta-present multiple times on a given
 * element, only the first restrictions of each declaration level will be applied,
 * but all the restrictions from different levels will be applied.
 * For example, if you have this annotation on both the class level and method level,
 * the method level annotation can only narrow the scope of the class level annotation.
 *
 * Exception: If the annotation is present on both the class and superclass,
 * the class-level annotation takes priority.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(ExecutionOnOsCondition::class)
annotation class OsCondition(
    val supportedOn: Array<OS> = [OS.LINUX, OS.MAC, OS.WINDOWS],
    val enabledOnCI: Array<OS> = [OS.LINUX, OS.WINDOWS],
)

internal class ExecutionOnOsCondition : ExecutionCondition {

    private val logger = LoggerFactory.getLogger(ExecutionOnOsCondition::class.java)

    private val isUnderTeamcity = System.getenv("TEAMCITY_VERSION") != null

    private val enabledOnCurrentOs = "Enabled on operating system: ${System.getProperty("os.name")}"
    private val notSupportedOnCurrentOs = "Test is not supported on operating system: ${System.getProperty("os.name")}"
    private val disabledForCI = "Disabled for operating system: ${System.getProperty("os.name")} on CI"

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val annotation = findAnnotation<OsCondition>(context)

        val supportedOn = annotation.supportedOn
        val enabledOnCI = annotation.enabledOnCI

        return if (supportedOn.none { it.isCurrentOs }) {
            logger.info { createDisabledMessage(context.element.get(), "local", supportedOn) }
            ConditionEvaluationResult.disabled(notSupportedOnCurrentOs)
        } else if (isUnderTeamcity && enabledOnCI.none { it.isCurrentOs }) {
            logger.info { createDisabledMessage(context.element.get(), "TeamCity", enabledOnCI) }
            ConditionEvaluationResult.disabled(disabledForCI)
        } else {
            ConditionEvaluationResult.enabled(enabledOnCurrentOs)
        }
    }

    private fun createDisabledMessage(annotatedElement: AnnotatedElement, environment: String, allowedOS: Array<OS>): String {
        return "$annotatedElement" +
                " was disabled in the $environment environment" +
                " for the current os=${OS.current()}," +
                " because allowed environments are: ${allowedOS.joinToString(separator = ", ") { it.name }}"
    }
}