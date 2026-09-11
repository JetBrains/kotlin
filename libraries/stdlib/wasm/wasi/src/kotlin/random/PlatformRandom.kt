/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.random

private fun wasiRandomGet(): Long {
    // TODO(REVIEW): this is a cryptographically secure random number generator. Doens't have to be available on all platforms.
    //               We could also instead use `Insecure.getInsecureRandomU64()`. Should we?
    return stdlib.wit.bindings.Random.getRandomU64().toLong()
}

internal actual fun defaultPlatformRandom(): Random = Random(wasiRandomGet())
