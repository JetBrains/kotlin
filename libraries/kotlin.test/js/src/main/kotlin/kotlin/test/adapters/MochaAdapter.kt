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

package kotlin.test.adapters

/**
 * Mocha adapter
 */
internal class MochaAdapter : FrameworkAdapter {
    override fun suite(name: String, suiteFn: () -> Unit) {
        describe(name, suiteFn)
    }

    override fun xsuite(name: String, suiteFn: () -> Unit) {
        xdescribe(name, suiteFn)
    }

    override fun fsuite(name: String, suiteFn: () -> Unit) {
        js("describe.only")(name, suiteFn)
    }

    override fun test(name: String, testFn: () -> Unit) {
        it(name, testFn)
    }

    override fun xtest(name: String, testFn: () -> Unit) {
        xit(name, testFn)
    }

    override fun ftest(name: String, testFn: () -> Unit) {
        js("it.only")(name, testFn)
    }
}