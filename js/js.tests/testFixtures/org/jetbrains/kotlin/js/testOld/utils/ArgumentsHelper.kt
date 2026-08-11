/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.testOld.utils

import org.jetbrains.kotlin.test.TargetBackend

/**
 * Arguments format: `((namedArg|positionalArg)\s+)*`
 *
 * Where:
 *   - `namedArg` is `key=value` or `key="spaced value"`
 *   - `positionalArg` is `value`
 *
 * Neither key, nor value should contain spaces.
 */
class ArgumentsHelper(private val entry: String) {
    companion object {
        private val argumentsPattern = Regex($$"""[\w$_;.]+(=((".*?")|[\w$_;.]+))?""")
    }
    private val positionalArguments: MutableList<String> = mutableListOf()
    private val namedArguments: MutableMap<String, String> = hashMapOf()

    init {
        for (match in argumentsPattern.findAll(entry)) {
            val argument = match.value
            val keyVal = argument.split("=", limit = 2)
            when (keyVal.size) {
                1 -> positionalArguments.add(keyVal[0])
                2 -> {
                    var value = keyVal[1]
                    if (value.startsWith('"') && value.endsWith('"')) {
                        value = value.substring(1, value.length - 1)
                    }
                    namedArguments[keyVal[0]] = value
                }
                else -> throw IllegalArgumentException("Wrong argument format: $argument")
            }
        }
    }

    private val targetBackends: Set<TargetBackend> =
        findNamedListArgument("TARGET_BACKENDS").mapTo(hashSetOf(), TargetBackend::valueOf)

    private val ignoredBackends: Set<TargetBackend> =
        findNamedListArgument("IGNORED_BACKENDS").mapTo(hashSetOf(), TargetBackend::valueOf)

    fun shouldRunWithBackend(backend: TargetBackend): Boolean {
        if (targetBackends.isNotEmpty()) {
            return backend in targetBackends
        }
        return backend !in ignoredBackends
    }

    val first: String
        get() = getPositionalArgument(0)

    fun getPositionalArgument(index: Int): String {
        require(positionalArguments.size > index) { "Argument at index `" + index + "` not found in entry: " + entry }
        return positionalArguments[index]
    }

    fun getNamedArgument(name: String): String =
        requireNotNull(namedArguments[name]) { "Argument `$name` not found in entry: $entry" }

    fun findNamedArgument(name: String): String? =
        namedArguments[name]

    fun findNamedListArgument(name: String): List<String> {
        val value = findNamedArgument(name) ?: return emptyList()
        return value.split(";")
    }

    override fun toString(): String = entry
}
