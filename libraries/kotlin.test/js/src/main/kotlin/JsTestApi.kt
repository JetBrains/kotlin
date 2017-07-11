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

import kotlin.test.*

/**
 * Functions in this file are exposed in the root package to simplify their use from JavaScript.
 * For example: require('kotlin-test').setAdapter({ /* Your custom [FrameworkAdapter] here */ });
 */

/**
 * Overrides the framework adapter with the provided instance of a [FrameworkAdapter]. Use in order to add support to a
 * custom test framework.
 *
 * Also some string arguments are supported. Use "qunit" to set the adapter to [QUnit](https://qunitjs.com/), "mocha" for
 * [Mocha](https://mochajs.org/), "jest" for [Jest](https://facebook.github.io/jest/),
 * "jasmine" for [Jasmine](https://github.com/jasmine/jasmine), and "auto" to detect one of those frameworks automatically.
 *
 * If this function is not called, the test framework will be detected automatically (as if "auto" was passed).
 *
 */
@JsName("setAdapter")
internal fun setAdapter(adapter: dynamic) = kotlin.test.setAdapter(adapter)

/**
 * Use in order to define which action should be taken by the test framework on the testResult.
 */
@JsName("setAssertHook")
internal fun setAssertHook(hook: (AssertionResult) -> Unit) = kotlin.test.setAssertHook(hook)
