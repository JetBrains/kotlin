/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package foo

import org.jetbrains.kotlin.plugin.sandbox.DummyFunction

@DummyFunction()
class A

fun main() {
    dummyA(A())
}
