/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics.arguments

import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StatisticsValuesConsumer
import org.jetbrains.kotlin.statistics.metrics.StringListMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * A single declarative rule: for compilations whose compiler arguments are an [argumentsClass], read something out of
 * those arguments and report zero or more FUS metrics.
 *
 * A rule declared for a base class applies to every subclass, so a rule on `CommonCompilerArguments` is reported for
 * every Kotlin compilation, one on `CommonKlibBasedCompilerArguments` for Native/JS/Wasm, and so on.
 */
internal class TrackedCompilerArgument<A : CommonToolArguments>(
    val argumentsClass: KClass<A>,
    /**
     * CLI names of the arguments this rule reads, for example `["-jvm-default", "-Xjvm-default"]`.
     */
    val cliArguments: List<String>,
    private val reportImplicit: Boolean,
    val condition: (A) -> Boolean,
    private val reporter: (A, StatisticsValuesConsumer) -> Unit,
) {
    fun reportIfApplicable(arguments: CommonToolArguments, consumer: StatisticsValuesConsumer) {
        if (!argumentsClass.isInstance(arguments)) return
        if (reportImplicit || arguments.wasExplicitlyPassed(cliArguments)) {
            @Suppress("UNCHECKED_CAST")
            val args = arguments as A
            if (condition(args)) {
                reporter(args, consumer)
            }
        }
    }

    override fun toString(): String = "${argumentsClass.simpleName}: ${cliArguments.joinToString("/")}"
}

private fun CommonToolArguments.wasExplicitlyPassed(cliNames: List<String>): Boolean =
    explicitArguments.keys.any { field -> cliNames.any { it == field.argument.value || it == field.argument.deprecatedName } }

internal fun trackedCompilerArguments(declare: TrackedArgumentsBuilder.() -> Unit): List<TrackedCompilerArgument<*>> {
    return TrackedArgumentsBuilder().apply(declare).build()
}

internal class TrackedArgumentsBuilder{
    private val trackedArguments = mutableListOf<TrackedCompilerArgument<*>>()

    inline fun <reified A : CommonToolArguments> forArguments(declare: TrackedArgumentsScope<A>.() -> Unit, ) {
        TrackedArgumentsScope(A::class, condition = { true }).declare()
    }

    /**
     * Rule declarations for compiler arguments of type [A].
     *
     * `null` returned anywhere in an `extract`/`convert` chain means "do not report anything".
     */
    inner class TrackedArgumentsScope<A : CommonToolArguments>(
        private val argumentsClass: KClass<A>,
        private val condition: (A) -> Boolean,
    ) {
        fun booleanMetric(
            metric: BooleanMetrics,
            vararg cliArguments: String,
            allowImplicit: Boolean = false,
            extract: (A) -> Boolean?,
        ) {
            register(cliArguments, allowImplicit) { arguments, consumer ->
                extract(arguments)?.let { consumer.report(metric, it) }
            }
        }

        fun booleanMetric(
            metric: BooleanMetrics,
            argument: KProperty1<A, Boolean>,
            allowImplicit: Boolean = false,
        ) {
            booleanMetric(
                metric,
                argument.cliArgument,
                allowImplicit = allowImplicit,
                extract = { argument.get(it) }
            )
        }

        fun numberMetric(
            metric: NumericalMetrics,
            vararg cliArguments: String,
            allowImplicit: Boolean = false,
            extract: (A) -> Long?,
        ) {
            register(cliArguments, allowImplicit) { arguments, consumer ->
                extract(arguments)?.let { consumer.report(metric, it) }
            }
        }

        fun numberMetric(
            metric: NumericalMetrics,
            argument: KProperty1<A, String>,
        ) {
            numberMetric(
                metric,
                argument.cliArgument,
                extract = { argument.get(it).toLongOrNull() }
            )
        }

        fun <V : Any> stringMetric(
            metric: StringMetrics,
            vararg cliArguments: String,
            allowImplicit: Boolean = false,
            extract: (A) -> V?,
            convert: (V) -> String? = { it.toString() },
        ) {
            register(cliArguments, allowImplicit) { arguments, consumer ->
                extract(arguments)?.let(convert)?.let { consumer.report(metric, it) }
            }
        }

        fun stringMetric(
            metric: StringMetrics,
            argument: KProperty1<A, String>,
        ) {
            stringMetric(
                metric,
                argument.cliArgument,
                extract = { argument.get(it) },
                convert = { it }
            )
        }

        fun stringMetric(
            metric: StringMetrics,
            argument: KProperty1<A, String?>,
            default: String? = null,
        ) {
            stringMetric(
                metric,
                argument.cliArgument,
                allowImplicit = default != null,
                extract = { argument.get(it) ?: default },
                convert = { it }
            )
        }

        fun <V : Any> stringListMetric(
            metric: StringListMetrics,
            vararg cliArguments: String,
            allowImplicit: Boolean = false,
            extract: (A) -> V?,
            convert: (V) -> String? = { it.toString() },
        ) {
            register(cliArguments, allowImplicit) { arguments, consumer ->
                extract(arguments)?.let(convert)?.let { consumer.report(metric, it) }
            }
        }

        @JvmName("stringList_Boolean")
        fun stringListMetric(
            metric: StringListMetrics,
            argument: KProperty1<A, Boolean>,
            allowImplicit: Boolean = false,
        ) {
            stringListMetric(
                metric,
                argument.cliArgument,
                allowImplicit = allowImplicit,
                extract = { argument.get(it) },
                convert = { it.toString() },
            )
        }

        @JvmName("stringList_String")
        fun stringListMetric(
            metric: StringListMetrics,
            argument: KProperty1<A, String>,
        ) {
            stringListMetric(
                metric,
                argument.cliArgument,
                extract = { argument.get(it) },
                convert = { it },
            )
        }

        @JvmName("stringList_StringArray")
        fun stringListMetric(
            metric: StringListMetrics,
            argument: KProperty1<A, Array<String>>,
        ) {
            stringListMetric(
                metric,
                argument.cliArgument,
                extract = { argument.get(it) },
                convert = { it.joinToString(",") },
            )
        }

        /**
         * Reports the [BooleanMetrics] chosen by [selectMetric] with `true`, for arguments whose *value* decides
         * which metric is meant, like `-Xbinary=gc=noop` reporting `ENABLED_NOOP_GC`.
         *
         * Nothing is reported when [selectMetric] returns `null`.
         */
        fun selectFlagMetric(
            argument: KProperty1<A, *>,
            selectMetric: (A) -> BooleanMetrics?,
        ) {
            selectFlagMetric(argument.cliArgument, selectMetric = selectMetric)
        }

        fun selectFlagMetric(
            vararg cliArguments: String,
            allowImplicit: Boolean = false,
            selectMetric: (A) -> BooleanMetrics?,
        ) {
            register(cliArguments, allowImplicit) { arguments, consumer ->
                selectMetric(arguments)?.let { consumer.report(it, true) }
            }
        }

        /**
         * A [StringMetrics] whose argument value needs converting, like the boolean `-Xir-per-module` becoming
         * `"per_module"` or `"whole_program"`.
         */
        fun <V : Any> stringMetric(
            metric: StringMetrics,
            argument: KProperty1<A, V>,
            allowImplicit: Boolean = false,
            convert: (V) -> String?,
        ) {
            stringMetric(
                metric,
                argument.cliArgument,
                allowImplicit = allowImplicit,
                extract = { argument.get(it) },
                convert = convert,
            )
        }

        fun withCondition(condition: (A) -> Boolean, declare: TrackedArgumentsScope<A>.() -> Unit) {
            TrackedArgumentsScope(argumentsClass) {
                this.condition(it) && condition(it)
            }.apply(declare)
        }

        private fun register(cliArguments: Array<out String>, allowImplicit: Boolean, reporter: (A, StatisticsValuesConsumer) -> Unit) {
            trackedArguments += TrackedCompilerArgument(argumentsClass, cliArguments.toList(), allowImplicit, condition, reporter)
        }
    }

    fun build(): List<TrackedCompilerArgument<*>> {
        return trackedArguments.toList()
    }
}
