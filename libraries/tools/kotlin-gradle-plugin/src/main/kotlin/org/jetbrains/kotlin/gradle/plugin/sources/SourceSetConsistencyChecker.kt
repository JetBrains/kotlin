/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.InvalidUserDataException
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

internal class ConsistencyCheck<T, S>(
    val name: String,
    val getValue: (T) -> S,
    val leftExtendsRightConsistently: (S, S) -> Boolean,
    val consistencyConditionHint: String
)

object SourceSetConsistencyChecks {
    private val defaultLanguageVersion = LanguageVersion.LATEST_STABLE

    const val languageVersionCheckHint =
        "The language version of the dependent source set must be greater than or equal to that of its dependency."

    internal val languageVersionCheck = ConsistencyCheck<KotlinSourceSet, LanguageVersion>(
        name = "language version",
        getValue = { sourceSet ->
            sourceSet.languageSettings.languageVersion?.let { parseLanguageVersionSetting(it) } ?: defaultLanguageVersion
        },
        leftExtendsRightConsistently = { left, right -> left >= right },
        consistencyConditionHint = languageVersionCheckHint
    )

    const val unstableFeaturesHint = "The dependent source set must enable all unstable language features that its dependency has."

    internal val unstableFeaturesCheck = ConsistencyCheck<KotlinSourceSet, Set<LanguageFeature>>(
        name = "unstable language feature set",
        getValue = { sourceSet ->
            sourceSet.languageSettings.enabledLanguageFeatures
                .map { parseLanguageFeature(it)!! }
                .filterTo(mutableSetOf()) { it.kind == LanguageFeature.Kind.UNSTABLE_FEATURE }
        },
        leftExtendsRightConsistently = { left, right -> left.containsAll(right) },
        consistencyConditionHint = unstableFeaturesHint
    )
}

internal class SourceSetConsistencyChecker(
    val checks: List<ConsistencyCheck<KotlinSourceSet, *>>
) {
    fun <S> runSingleCheck(
        dependent: KotlinSourceSet,
        dependency: KotlinSourceSet,
        check: ConsistencyCheck<KotlinSourceSet, S>
    ) {
        val leftValue = check.getValue(dependent)
        val rightValue = check.getValue(dependency)

        if (!check.leftExtendsRightConsistently(leftValue, rightValue)) {
            throw InvalidUserDataException(
                "Inconsistent settings for Kotlin source sets: '${dependent.name}' depends on '${dependency.name}'\n" +
                        "'${dependent.name}': ${check.name} is ${leftValue}\n" +
                        "'${dependency.name}': ${check.name} is ${rightValue}\n" +
                        check.consistencyConditionHint
            )
        }
    }

    fun runAllChecks(dependent: KotlinSourceSet, dependency: KotlinSourceSet) {
        for (check in checks) {
            runSingleCheck(dependent, dependency, check)
        }
    }
}

internal val defaultSourceSetLanguageSettingsChecker = with(SourceSetConsistencyChecks) {
    // We don't check the progressive mode, since the features it enables are bugfixes
    SourceSetConsistencyChecker(listOf(languageVersionCheck, unstableFeaturesCheck))
}