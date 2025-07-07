/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 *
 * This file is based on code from the Gradle project (https://github.com/gradle/common-custom-user-data-gradle-plugin)
 * Original class: com.gradle.CiUtils
 * License: Apache License 2.0
 *
 * Modifications:
 * - converted to Kotlin file
 * - use System.getenv method instead of com.gradle.Utils.envVariable as minimal supported Gradle version for current Kotlin is 7.6
 * - use System.getProperty instead of com.gradle.Utils.sysProperty
 * - return String? property name instead of Boolean value when CI variable is detected
 * - add detectedCiProperty method to return detected CI property name or null
 *
 * Copyright [Original Gradle authors]
 */

package org.jetbrains.kotlin.gradle.fus.internal

fun isCiBuild() = detectedCiProperty() != null
fun detectedCiProperty() = isGenericCI()
    ?: isJenkins()
    ?: isHudson()
    ?: isTeamCity()
    ?: isCircleCI()
    ?: isBamboo()
    ?: isGitHubActions()
    ?: isGitLab()
    ?: isTravis()
    ?: isBitrise()
    ?: isGoCD()
    ?: isAzurePipelines()
    ?: isBuildkite()

fun isGenericCI(): String? = isEnvironmentVariablePresent("CI")

fun isJenkins(): String? = isEnvironmentVariablePresent("JENKINS_URL")

fun isHudson(): String? = isEnvironmentVariablePresent("HUDSON_URL")

fun isTeamCity(): String? = isEnvironmentVariablePresent("TEAMCITY_VERSION")

fun isCircleCI(): String? = isEnvironmentVariablePresent("CIRCLE_BUILD_URL")

fun isBamboo(): String? = isEnvironmentVariablePresent("bamboo_resultsUrl")

fun isGitHubActions(): String? = isEnvironmentVariablePresent("GITHUB_ACTIONS")

fun isGitLab(): String? = isEnvironmentVariablePresent("GITLAB_CI")

fun isTravis(): String? = isEnvironmentVariablePresent("TRAVIS_JOB_ID")

fun isBitrise(): String? = isEnvironmentVariablePresent("BITRISE_BUILD_URL")

fun isGoCD(): String? = isEnvironmentVariablePresent("GO_SERVER_URL")

fun isAzurePipelines(): String? = isEnvironmentVariablePresent("TF_BUILD")

fun isBuildkite(): String? = isEnvironmentVariablePresent("BUILDKITE")

private fun isEnvironmentVariablePresent(name: String) = if ((System.getenv(name) ?: System.getProperty(name)) != null) {
    name
} else {
    null
}
