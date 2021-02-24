/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.dependencies.impl

import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.ExternalDependenciesResolver

private val nameRegex = Regex("^[^\\S\\r\\n]*([a-zA-Z][a-zA-Z0-9-_]*)\\b")
private val valueRegex = Regex("^[^\\S\\r\\n]*([a-zA-Z0-9-_,]+)\\b")
private val equalsRegex = Regex("^[^\\S\\r\\n]*=")

/**
 * Simple lightweight options parser for external dependency resolvers.
 *
 * This parser expects the input to be a series of equality statements:
 * `foo=Foo bar=Bar`
 *
 * And additionally supports flags without any equality statement:
 * `foo bar`
 */
internal object SimpleExternalDependenciesResolverOptionsParser {
    private sealed class Token {
        data class Name(val name: String) : Token()
        data class Value(val value: String) : Token()
        object Equals
    }

    private class Scanner(text: String) {
        private var consumed = ""

        var remaining = text
            private set

        private fun take(regex: Regex) = regex
            .find(remaining)
            ?.also { match ->
                consumed += match.value
                remaining = remaining.removePrefix(match.value)
            }

        fun takeName(): Token.Name? = take(nameRegex)?.let { Token.Name(it.groups[1]!!.value) }
        fun takeValue(): Token.Value? = take(valueRegex)?.let { Token.Value(it.groups[1]!!.value) }
        fun takeEquals(): Token.Equals? = take(equalsRegex)?.let { Token.Equals }

        fun hasFinished(): Boolean = remaining.isBlank()
    }

    operator fun invoke(
        vararg options: String,
        locationWithId: SourceCode.LocationWithId? = null
    ): ResultWithDiagnostics<ExternalDependenciesResolver.Options> {

        val map = mutableMapOf<String, String>()

        for (option in options) {
            val scanner = Scanner(option)

            while (!scanner.hasFinished()) {
                val name = scanner.takeName()?.name ?: return makeFailureResult(
                    "Failed to parse options from annotation. Expected a valid option name but received:\n${scanner.remaining}",
                    locationWithId
                )

                if (scanner.takeEquals() != null) {
                    // TODO: Consider supporting string literals
                    val value = scanner.takeValue()?.value ?: return makeFailureResult(
                        "Failed to parse options from annotation. Expected a valid option value but received:\n${scanner.remaining}",
                        locationWithId
                    )

                    map.tryToAddOption(name, value)?.let { return it }
                } else {
                    map.tryToAddOption(name, "true")?.let { return it }
                }
            }
        }

        return makeExternalDependenciesResolverOptions(map).asSuccess()
    }
}

private fun <K, V> MutableMap<K, V>.tryToAddOption(
    key: K,
    value: V,
    locationWithId: SourceCode.LocationWithId? = null
): ResultWithDiagnostics.Failure? = when (val previousValue = this[key]) {
    null, value -> {
        this[key] = value
        null
    }
    else -> makeFailureResult("Conflicting values for option $key: $previousValue and $value", locationWithId)
}