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

    /**
     * A test suite. [kotlin.test.suite] delegates to this function.
     *
     * @param name the name of the test suite, e.g. a class name
     * @param ignored whether the test suite is ignored, e.g. marked with [Ignore] annotation
     * @param suiteFn defines nested suites by calling [kotlin.test.suite] and tests by calling [kotlin.test.test]
     */
    fun suite(name: String, ignored: Boolean, suiteFn: () -> Unit)

    /**
     * A test. [kotlin.test.test] delegates to this function.
     *
     * @param name the test name.
     * @param ignored whether the test is ignored
     * @param testFn contains test body invocation
     */
    fun test(name: String, ignored: Boolean, testFn: () -> Unit)
}
