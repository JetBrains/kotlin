/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

/**
 * Functions in this file are exposed in the root package to simplify their use from JavaScript.
 * For example: require('kotlin-test').setAdapter({ /* Your custom [FrameworkAdapter] here */ });
 */

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
@JsName("setAdapter")
internal fun setAdapter(adapter: dynamic) = kotlin.test.setAdapter(adapter)