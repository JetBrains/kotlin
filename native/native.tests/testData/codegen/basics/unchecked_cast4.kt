/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    CI1I2().uncheckedCast<CI1I2>()
    CI1I2().uncheckedCast<OtherCI1I2>()

    assertFailsWith<ClassCastException> {
        Any().uncheckedCast<CI1I2>()
    }

    return "OK"
}

fun <R : C> Any?.uncheckedCast() where R : I1, R : I2 {
    this as R
}

interface I1
interface I2
open class C

class CI1I2 : C(), I1, I2
class OtherCI1I2 : C(), I1, I2