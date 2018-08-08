/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.InvalidUserDataException
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet

internal class ConsistencyCheck<T, S>(
    val name: String,
    val getValue: (T) -> S,
    val leftExtendsRightConsistently: (S, S) -> Boolean,
    val consistencyConditionHint: String
)

private val languageVersionCheck = ConsistencyCheck<KotlinSourceSet, LanguageVersion>(
    name = "language version",
    getValue = { sourceSet ->
        sourceSet.languageSettings.languageVersion?.let { parseLanguageVersionSetting(it) } ?: LanguageVersion.LATEST_STABLE
    },
    leftExtendsRightConsistently = { left, right -> left >= right },
    consistencyConditionHint = "The language version of the dependent source set must be greater than or equal to that of its dependency."
)

private val apiVersionCheck = ConsistencyCheck<KotlinSourceSet, ApiVersion>(
    name = "language version",
    getValue = { sourceSet ->
        sourceSet.languageSettings.apiVersion?.let { parseApiVersionSettings(it) } ?: ApiVersion.LATEST_STABLE
    },
    leftExtendsRightConsistently = { left, right -> left >= right },
    consistencyConditionHint = "The language version of the dependent source set must be greater than or equal to that of its dependency."
)

internal class SourceSetConsistencyChecker(
    val checks: Set<ConsistencyCheck<KotlinSourceSet, *>>
) {
    fun <S> runSingleCheck(
        left: KotlinSourceSet,
        right: KotlinSourceSet,
        check: ConsistencyCheck<KotlinSourceSet, S>
    ) {
        val leftValue = check.getValue(left)
        val rightValue = check.getValue(right)

        if (!check.leftExtendsRightConsistently(leftValue, rightValue)) {
            throw InvalidUserDataException(
                "Inconsistent language settings for Kotlin source sets: '${left.name}' depends on '${right.name}'\n" +
                        "'${left.name}': ${check.name} is ${leftValue}\n" +
                        "'${right.name}': ${check.name} is ${rightValue}\n" +
                        check.consistencyConditionHint
            )
        }
    }

    fun runAllChecks(left: KotlinSourceSet, right: KotlinSourceSet) {
        for (check in checks) {
            runSingleCheck(left, right, check)
        }
    }
}