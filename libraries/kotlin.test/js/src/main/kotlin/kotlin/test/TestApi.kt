/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.test.adapters.*

/**
 * Overrides current framework adapter with a provided instance of [FrameworkAdapter]. Use in order to support custom test frameworks.
 *
 * Also some string arguments are supported. Use "qunit" to set the adapter to [QUnit](https://qunitjs.com/), "mocha" for
 * [Mocha](https://mochajs.org/), "jest" for [Jest](https://facebook.github.io/jest/),
 * "jasmine" for [Jasmine](https://github.com/jasmine/jasmine), and "auto" to detect one of those frameworks automatically.
 *
 * If this function is not called, the test framework will be detected automatically (as if "auto" was passed).
 *
 */
internal fun setAdapter(adapter: dynamic) {
    if (js("typeof adapter === 'string'")) {
        NAME_TO_ADAPTER[adapter]?.let {
            setAdapter(it.invoke())
        } ?: throw IllegalArgumentException("Unsupported test framework adapter: '$adapter'")
    } else {
        currentAdapter = adapter
    }
}

/**
 * Use in order to define which action should be taken by the test framework on the [AssertionResult].
 */
internal fun setAssertHook(hook: (AssertionResult) -> Unit) {
    assertHook = hook
}


/**
 * The functions below are used by the compiler to describe the tests structure, e.g.
 *
 * suite('a suite', false, function() {
 *   suite('a subsuite', false, function() {
 *     test('a test', false, function() {...});
 *     test('an ignored/pending test', true, function() {...});
 *   });
 *   suite('an ignored/pending test', true, function() {...});
 * });
 */

@JsName("suite")
internal fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
    adapter().suite(name, ignored, suiteFn)
}

@JsName("test")
internal fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
    adapter().test(name, ignored, testFn)
}

internal var currentAdapter: FrameworkAdapter? = null

internal fun adapter(): FrameworkAdapter {
    val result = currentAdapter ?: detectAdapter()
    currentAdapter = result
    return result
}

@JsName("kotlinTest")
public external val kotlinTestNamespace: KotlinTestNamespace

public external interface KotlinTestNamespace {
    public val adapterTransformer: ((FrameworkAdapter) -> FrameworkAdapter)?
}

internal fun detectAdapter(): FrameworkAdapter {
    val frameworkAdapter = when {
        isQUnit() -> QUnitAdapter()
        isJasmine() -> JasmineLikeAdapter()
        else -> BareAdapter()
    }
    return if (jsTypeOf(kotlinTestNamespace) != "undefined") {
        val adapterTransform = kotlinTestNamespace
            .adapterTransformer
        if (adapterTransform !== null) {
            adapterTransform(frameworkAdapter)
        } else frameworkAdapter
    } else frameworkAdapter
}

internal val NAME_TO_ADAPTER: Map<String, () -> FrameworkAdapter> = mapOf(
    "qunit" to ::QUnitAdapter,
    "jasmine" to ::JasmineLikeAdapter,
    "mocha" to ::JasmineLikeAdapter,
    "jest" to ::JasmineLikeAdapter,
    "auto" to ::detectAdapter
)