/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.test.FrameworkAdapter

// Need to wrap into additional lambdas so that js launcher will work without Mocha or any other testing framework
@JsFun("(name, fn) => describe(name, fn)")
internal external fun describe(name: String, fn: () -> Unit)

@JsFun("(name, fn) => xdescribe(name, fn)")
internal external fun xdescribe(name: String, fn: () -> Unit)

@JsFun("(name, fn) => it(name, fn)")
internal external fun it(name: String, fn: () -> Any?)

@JsFun("(name, fn) => xit(name, fn)")
internal external fun xit(name: String, fn: () -> Any?)

/**
 * [Jasmine](https://github.com/jasmine/jasmine) adapter.
 * Also used for [Mocha](https://mochajs.org/) and [Jest](https://facebook.github.io/jest/).
 */
internal class JasmineLikeAdapter : FrameworkAdapter {
    override fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit) {
        if (ignored) {
            xdescribe(name, suiteFn)
        } else {
            describe(name, suiteFn)
        }
    }

    override fun test(name: String, ignored: Boolean, testFn: () -> Any?) {
        if (ignored) {
            xit(name, testFn)
        } else {
            it(name, testFn)
        }
    }
}