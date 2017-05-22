/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import kotlin.test.okFun

fun test(name: String, testFn: () -> Unit) {
    JasmineAdapter.test(name, testFn)
}

interface FrameworkAdapter {
    fun test(name: String, testFn: () -> Unit)
}

object QUnitAdapter : FrameworkAdapter {
    override fun test(name: String, testFn: () -> Unit) {
        QUnit.test(name) { assert ->
            okFun = { actual, message -> assert.ok(actual, message) }
            testFn()
        }
    }
}

object JasmineAdapter : FrameworkAdapter {
    override fun test(name: String, testFn: () -> Unit) {
        describe(name) {
            it("should be ok") {
                testFn()
            }
        }
    }
}

/**
 * The [QUnit](http://qunitjs.com/) API
 */
external object QUnit {
    fun test(name: String, testFn: (dynamic) -> Unit): Unit
}

/**
 * Jasmine/Mocha API
 */
external fun describe(name: String, fn: () -> Unit)
external fun it(name: String, fn: () -> Unit)