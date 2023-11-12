/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

/**
 * Default [Asserter] implementation to avoid dependency on JUnit or TestNG.
 */
public object DefaultAsserter : Asserter {
    override fun fail(message: String?): Nothing {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }

    @SinceKotlin("1.4")
    override fun fail(message: String?, cause: Throwable?): Nothing {
        throw AssertionErrorWithCause(message, cause)
    }
}