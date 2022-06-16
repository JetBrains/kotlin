/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(FreezingIsDeprecated::class)

import kotlin.test.*
import kotlin.native.concurrent.*
import kotlin.native.internal.Frozen

class NonFrozenClass

@Frozen
class FrozenClass

val globalNonFrozen = NonFrozenClass()
@SharedImmutable
val sharedImmutableNonFrozen = NonFrozenClass()

val globalFrozen = FrozenClass()
@SharedImmutable
val sharedImmutableFrozen = FrozenClass()

fun main(args: Array<String>) {
    val mode = args.first()
    val localNonFrozen = NonFrozenClass()
    val localFrozen = FrozenClass()
    
    when (mode) {
        "full" -> {
            checkFreezeCheck(localNonFrozen, false, true)
            checkFreezeCheck(localFrozen, true, true)
            checkFreezeCheck(globalNonFrozen, false, true)
            checkFreezeCheck(sharedImmutableNonFrozen, true, true)
            checkFreezeCheck(globalFrozen, true, true)
            checkFreezeCheck(sharedImmutableFrozen, true, true)
        }
        "disabled" -> {
            checkFreezeCheck(localNonFrozen, false, false)
            checkFreezeCheck(localFrozen, false, false)
            checkFreezeCheck(globalNonFrozen, false, false)
            checkFreezeCheck(sharedImmutableNonFrozen, false, false)
            checkFreezeCheck(globalFrozen, false, false)
            checkFreezeCheck(sharedImmutableFrozen, false, false)
        }
        "explicitOnly" -> {
            checkFreezeCheck(localNonFrozen, false, true)
            checkFreezeCheck(localFrozen, false, true)
            checkFreezeCheck(globalNonFrozen, false, true)
            checkFreezeCheck(sharedImmutableNonFrozen, false, true)
            checkFreezeCheck(globalFrozen, false, true)
            checkFreezeCheck(sharedImmutableFrozen, false, true)
        }
    }
}

private fun checkFreezeCheck(arg: Any, isFrozenBefore: Boolean, isFrozenAfter: Boolean) {
    assertEquals(isFrozenBefore, arg.isFrozen)
    arg.freeze()
    assertEquals(isFrozenAfter, arg.isFrozen)
}