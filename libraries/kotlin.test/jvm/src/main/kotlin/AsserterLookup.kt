/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import java.util.ServiceLoader

private val contributors = ServiceLoader.load(AsserterContributor::class.java).toList()

internal actual fun lookupAsserter(): Asserter {
    for (contributor in contributors) {
        val asserter = contributor.contribute()
        if (asserter != null) return asserter
    }
    return DefaultAsserter
}