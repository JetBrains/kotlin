/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.reflect.KClass

/**
 * Takes the given [block] of test code and _doesn't_ execute it.
 *
 * This keeps the code under test referenced, but doesn't actually test it until it is implemented.
 */
public expect fun todo(block: () -> Unit)

/** Asserts that a [blockResult] is a failure with the specific exception type being thrown. */
@PublishedApi
internal expect fun <T : Throwable> checkResultIsFailure(exceptionClass: KClass<T>, message: String?, blockResult: Result<Unit>): T
