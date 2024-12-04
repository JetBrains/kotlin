/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.sources

import org.gradle.api.InvalidUserDataException
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.project.model.LanguageSettings

internal class ConsistencyCheck<T, S>(
    val name: String,
    val getValue: (T) -> S,
    val leftExtendsRightConsistently: (S, S) -> Boolean,
    val consistencyConditionHint: String
)

internal class FragmentConsistencyChecks<T>(
    unitName: String, // "fragment" or "source set"
    private val languageSettings: T.() -> LanguageSettings
) {
    private val defaultLanguageVersion = KotlinVersion.DEFAULT

    private val languageVersionCheckHint =
        "The language version of the dependent $unitName must be greater than or equal to that of its dependency."

    private val languageVersionCheck = ConsistencyCheck<T, KotlinVersion?>(
        name = "language version",
        getValue = { unit ->
            unit.languageSettings().getValueIfExists {
                languageVersion?.let { KotlinVersion.fromVersion(it) } ?: defaultLanguageVersion
            }
        },
        leftExtendsRightConsistently = { left, right ->
            if (left == null || right == null) true else left >= right
        },
        consistencyConditionHint = languageVersionCheckHint
    )

    private val unstableFeaturesHint = "The dependent $unitName must enable all unstable language features that its dependency has."

    private val unstableFeaturesCheck = ConsistencyCheck<T, Set<LanguageFeature>?>(
        name = "unstable language feature set",
        getValue = { unit ->
            unit.languageSettings().getValueIfExists {
                enabledLanguageFeatures
                    .mapNotNull { parseLanguageFeature(it) }
                    .filterTo(mutableSetOf()) { it.kind == LanguageFeature.Kind.UNSTABLE_FEATURE }
            }
        },
        leftExtendsRightConsistently = { left, right ->
            if (left == null || right == null) true else left.containsAll(right)
        },
        consistencyConditionHint = unstableFeaturesHint
    )

    private val optInAnnotationsInUseHint = "The dependent $unitName must use all opt-in annotations that its dependency uses."

    private val optInAnnotationsCheck = ConsistencyCheck<T, Set<String>?>(
        name = "set of opt-in annotations in use",
        getValue = { unit -> unit.languageSettings().getValueIfExists { optInAnnotationsInUse } },
        leftExtendsRightConsistently = { left, right ->
            if (left == null || right == null) true else left.containsAll(right)
        },
        consistencyConditionHint = optInAnnotationsInUseHint
    )

    val allChecks = listOf(languageVersionCheck, unstableFeaturesCheck, optInAnnotationsCheck)

    private fun <T> LanguageSettings.getValueIfExists(
        getValue: LanguageSettings.() -> T?
    ): T? {
        val defaultLanguageSettingsBuilder = this as DefaultLanguageSettingsBuilder
        return if (defaultLanguageSettingsBuilder.compilationCompilerOptions.isCompleted) {
            getValue(defaultLanguageSettingsBuilder)
        } else {
            null
        }
    }
}

internal fun parseLanguageFeature(featureName: String) = LanguageFeature.fromString(featureName)

internal class FragmentConsistencyChecker<T>(
    private val unitsName: String,
    private val name: T.() -> String,
    val checks: List<ConsistencyCheck<T, *>>
) {
    fun <S> runSingleCheck(
        dependent: T,
        dependency: T,
        check: ConsistencyCheck<T, S>
    ) {
        val leftValue = check.getValue(dependent)
        val rightValue = check.getValue(dependency)

        if (!check.leftExtendsRightConsistently(leftValue, rightValue)) {
            throw InvalidUserDataException(
                "Inconsistent settings for Kotlin $unitsName: '${dependent.name()}' depends on '${dependency.name()}'\n" +
                        "'${dependent.name()}': ${check.name} is ${leftValue}\n" +
                        "'${dependency.name()}': ${check.name} is ${rightValue}\n" +
                        check.consistencyConditionHint
            )
        }
    }

    fun runAllChecks(dependent: T, dependency: T) {
        for (check in checks) {
            runSingleCheck(dependent, dependency, check)
        }
    }
}
