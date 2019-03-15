/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

/**
 * Default [Asserter] implementation to avoid dependency on JUnit or TestNG.
 */
object DefaultAsserter : Asserter {
    override fun fail(message: String?): Nothing {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}

@Deprecated("DefaultAsserter is an object now, constructor call is not required anymore",
        ReplaceWith("DefaultAsserter", "kotlin.test.DefaultAsserter"))
@kotlin.js.JsName("DefaultAsserterConstructor")
@Suppress("FunctionName")
fun DefaultAsserter(): DefaultAsserter = DefaultAsserter