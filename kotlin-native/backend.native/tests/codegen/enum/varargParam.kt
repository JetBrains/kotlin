/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.varargParam

import kotlin.test.*

enum class Piece(vararg val states: Int) {
    I(3, 4, 5)
}

@Test fun runTest() {
    println(Piece.I.states[0])
}