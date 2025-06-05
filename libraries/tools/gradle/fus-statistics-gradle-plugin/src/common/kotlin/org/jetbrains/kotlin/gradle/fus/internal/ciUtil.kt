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
 * - use System.getenv method instead of System.getenv as minimal supported Gradle version for current Kotlin is 7.6
 * - use System.getProperty instead of com.gradle.Utils.sysProperty
 *
 * Copyright [Original Gradle authors]
 */

package org.jetbrains.kotlin.gradle.fus.internal

fun isCiBuild() = isGenericCI()
        || isJenkins()
        || isHudson()
        || isTeamCity()
        || isCircleCI()
        || isBamboo()
        || isGitHubActions()
        || isGitLab()
        || isTravis()
        || isBitrise()
        || isGoCD()
        || isAzurePipelines()
        || isBuildkite()


fun isGenericCI(): Boolean = (System.getenv("CI") ?: System.getProperty("CI")) == "true"

fun isJenkins(): Boolean = System.getenv("JENKINS_URL") == "true"

fun isHudson(): Boolean = System.getenv("HUDSON_URL") == "true"

fun isTeamCity(): Boolean = System.getenv("TEAMCITY_VERSION") == "true"

fun isCircleCI(): Boolean = System.getenv("CIRCLE_BUILD_URL") == "true"

fun isBamboo(): Boolean = System.getenv("bamboo_resultsUrl") == "true"

fun isGitHubActions(): Boolean = System.getenv("GITHUB_ACTIONS") == "true"

fun isGitLab(): Boolean = System.getenv("GITLAB_CI") == "true"

fun isTravis(): Boolean = System.getenv("TRAVIS_JOB_ID") == "true"

fun isBitrise(): Boolean = System.getenv("BITRISE_BUILD_URL") == "true"

fun isGoCD(): Boolean = System.getenv("GO_SERVER_URL") == "true"

fun isAzurePipelines(): Boolean = System.getenv("TF_BUILD") == "true"

fun isBuildkite(): Boolean = System.getenv("BUILDKITE") == "true"

