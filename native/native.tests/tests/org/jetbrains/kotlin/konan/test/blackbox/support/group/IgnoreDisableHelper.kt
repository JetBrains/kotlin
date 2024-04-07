/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.group

import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.GCScheduler
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.GCType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.OptimizationMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.PipelineType
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Settings
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.TestMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ThreadStateChecker
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.StringDirective
import org.jetbrains.kotlin.test.directives.model.ValueDirective

private val TARGET_FAMILY = "targetFamily"
private val TARGET_ARCHITECTURE = "targetArchitecture"
private val IS_APPLE_TARGET = "isAppleTarget"
private val CACHE_MODE_NAMES = CacheMode.Alias.entries.map { it.name }
private val TEST_MODE_NAMES = TestMode.entries.map { it.name }
private val OPTIMIZATION_MODE_NAMES = OptimizationMode.entries.map { it.name }
private val GC_TYPE_NAMES = GCType.entries.map { it.name }
private val GC_SCHEDULER_NAMES = GCScheduler.entries.map { it.name }
private val THREAD_STATE_CHECKER_NAMES = ThreadStateChecker.entries.map { it.name }
private val FAMILY_NAMES = Family.entries.map { it.name }
private val ARCHITECTURE_NAMES = Architecture.entries.map { it.name }
private val BOOLEAN_NAMES = listOf(true.toString(), false.toString())

// Note: this method would accept DISABLED_NATIVE without parameters as an unconditional test exclusion: don't even try to compile
internal fun Settings.isDisabledNative(directives: Directives) =
    evaluate(
        getDirectiveValues(
            TestDirectives.DISABLE_NATIVE, TestDirectives.DISABLE_NATIVE_K1, TestDirectives.DISABLE_NATIVE_K2,
            { directives.contains(it.name) },
            { directives.listValues(it.name) },
        )
    )

// Note: this method would ignore DISABLED_NATIVE without parameters, since it would be not a StringDirective, but new SimpleDirective
internal fun Settings.isDisabledNative(registeredDirectives: RegisteredDirectives) =
    evaluate(
        getDirectiveValues(
            TestDirectives.DISABLE_NATIVE, TestDirectives.DISABLE_NATIVE_K1, TestDirectives.DISABLE_NATIVE_K2,
            { registeredDirectives.contains(it) },
            { registeredDirectives.get(it) },
        )
    )

// Note: this method would treat IGNORE_NATIVE without parameters as an unconditional "test must fail on any config". Same as // IGNORE_BACKEND: NATIVE
internal fun Settings.isIgnoredWithIGNORE_NATIVE(directives: Directives) =
    evaluate(
        getDirectiveValues(
            TestDirectives.IGNORE_NATIVE, TestDirectives.IGNORE_NATIVE_K1, TestDirectives.IGNORE_NATIVE_K2,
            { directives.contains(it.name) },
            { directives.listValues(it.name) },
        )
    )

// Note: this method would ignore IGNORE_NATIVE without parameters, since it would be not a StringDirective, but new SimpleDirective
internal fun Settings.isIgnoredWithIGNORE_NATIVE(registeredDirectives: RegisteredDirectives) =
    evaluate(
        getDirectiveValues(
            TestDirectives.IGNORE_NATIVE, TestDirectives.IGNORE_NATIVE_K1, TestDirectives.IGNORE_NATIVE_K2,
            { registeredDirectives.contains(it) },
            { registeredDirectives.get(it) },
        )
    )

// Note: this method would treat IGNORE_NATIVE without parameters as an unconditional "test must fail on any config". Same as // IGNORE_BACKEND: NATIVE
internal fun Settings.isIgnoredTarget(directives: Directives): Boolean {
    return isIgnoredWithIGNORE_NATIVE(directives) || isIgnoredWithIGNORE_BACKEND { valueDirective ->
        directives.listValues(valueDirective.name)?.map { backendName ->
            enumValues<TargetBackend>().first { it.name == backendName }
        }
    }
}

// Note: this method would ignore IGNORE_NATIVE without parameters, since it would be not a StringDirective, but new SimpleDirective
internal fun Settings.isIgnoredTarget(registeredDirectives: RegisteredDirectives): Boolean {
    return isIgnoredWithIGNORE_NATIVE(registeredDirectives) || isIgnoredWithIGNORE_BACKEND { registeredDirectives.get(it) }
}

// Mimics `InTextDirectivesUtils.isIgnoredTarget(NATIVE, file)` but does not require file contents, but only already parsed directives.
private fun Settings.isIgnoredWithIGNORE_BACKEND(listValues: (ValueDirective<TargetBackend>) -> List<TargetBackend>?): Boolean {
    val containsNativeOrAny: (List<TargetBackend>) -> Boolean = { TargetBackend.NATIVE in it || TargetBackend.ANY in it }

    if (listValues(CodegenTestDirectives.IGNORE_BACKEND)?.let(containsNativeOrAny) == true)
        return true
    when (get<PipelineType>()) {
        PipelineType.K1 ->
            if (listValues(CodegenTestDirectives.IGNORE_BACKEND_K1)?.let(containsNativeOrAny) == true)
                return true
        PipelineType.K2 ->
            if (listValues(CodegenTestDirectives.IGNORE_BACKEND_K2)?.let(containsNativeOrAny) == true)
                return true
        else -> {}
    }
    return false
}


// Evaluation of conjunction of boolean expressions like `property1=value1 && property2=value2`.
// Any null element makes whole result as `true`.
internal fun Settings.evaluate(directiveValues: List<String?>): Boolean {
    directiveValues.forEach {
        if (it == null)
            return true  // Directive without value is treated as unconditional
        val split = it.split("&&")
        val booleanList = split.map {
            val matchResult = "(.+)=(.+)".toRegex().find(it.trim())
                ?: throw AssertionError("Invalid format for IGNORE_NATIVE* directive ($it). Must be <property>=<value>")
            val propName = matchResult.groups[1]?.value
            val (actualValue, supportedValues) = when (propName) {
                ClassLevelProperty.CACHE_MODE.shortName -> get<CacheMode>().alias.name to CACHE_MODE_NAMES
                ClassLevelProperty.TEST_MODE.shortName -> get<TestMode>().name to TEST_MODE_NAMES
                ClassLevelProperty.OPTIMIZATION_MODE.shortName -> get<OptimizationMode>().name to OPTIMIZATION_MODE_NAMES
                ClassLevelProperty.TEST_TARGET.shortName -> get<KotlinNativeTargets>().testTarget.name to null
                ClassLevelProperty.GC_TYPE.shortName -> get<GCType>().name to GC_TYPE_NAMES
                ClassLevelProperty.GC_SCHEDULER.shortName -> get<GCScheduler>().name to GC_SCHEDULER_NAMES
                ClassLevelProperty.USE_THREAD_STATE_CHECKER.shortName -> get<ThreadStateChecker>().name to THREAD_STATE_CHECKER_NAMES
                TARGET_FAMILY -> get<KotlinNativeTargets>().testTarget.family.name to FAMILY_NAMES
                TARGET_ARCHITECTURE -> get<KotlinNativeTargets>().testTarget.architecture.name to ARCHITECTURE_NAMES
                IS_APPLE_TARGET -> get<KotlinNativeTargets>().testTarget.family.isAppleFamily.toString() to BOOLEAN_NAMES
                else -> throw AssertionError("ClassLevelProperty name: $propName is not yet supported in IGNORE_NATIVE* test directives.")
            }
            val valueFromTestDirective = matchResult.groups[2]?.value!!
            supportedValues?.let {
                if (actualValue !in it)
                    throw AssertionError("Internal error: Test run value $propName=$actualValue is not in expected supported values: $it")
                if (valueFromTestDirective !in it)
                    throw AssertionError("Test directive `IGNORE_NATIVE*: $propName=$valueFromTestDirective` has unsupported value. Supported are: $it")
            }
            actualValue == valueFromTestDirective
        }
        val matches = booleanList.reduce { a, b -> a && b }
        if (matches)
            return true
    }
    return false
}

// Returns list of relevant directive values.
// Null is added to result list in case the directive given without value.
internal fun Settings.getDirectiveValues(
    directiveAllPipelineTypes: StringDirective,
    directiveK1: StringDirective,
    directiveK2: StringDirective,
    isSpecified: (StringDirective) -> Boolean,
    listValues: (StringDirective) -> List<String>?,
): List<String?> = buildList {
    fun extract(directive: StringDirective) {
        if (isSpecified(directive))
            listValues(directive)?.let { addAll(it) } ?: add(null)
    }
    extract(directiveAllPipelineTypes)
    when (get<PipelineType>()) {
        PipelineType.K1 -> extract(directiveK1)
        PipelineType.K2 -> extract(directiveK2)
        else -> {}
    }
}
