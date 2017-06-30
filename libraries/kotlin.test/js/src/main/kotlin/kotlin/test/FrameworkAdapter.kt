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

package kotlin.test

/**
 * Serves as a bridge to a testing framework.
 */
public external interface FrameworkAdapter {

    // A regular test suite.
    fun suite(name: String, suiteFn: () -> Unit)

    // An ignored test suite. Corresponds to a class marked with the `@Ignore` anotation.
    fun xsuite(name: String, suiteFn: () -> Unit)

    // A focused test suite. Corresponds to a class marked with the `@Only` annotation.
    fun fsuite(name: String, suiteFn: () -> Unit)

    // A regular test.
    fun test(name: String, testFn: () -> Unit)

    // An ignored test. Corresponds to a function marked with the `@Ignore` annotation.
    fun xtest(name: String, testFn: () -> Unit)

    // A focused test. Corresponds to a function marked with the `@Only` annotation.
    fun ftest(name: String, testFn: () -> Unit)
}
