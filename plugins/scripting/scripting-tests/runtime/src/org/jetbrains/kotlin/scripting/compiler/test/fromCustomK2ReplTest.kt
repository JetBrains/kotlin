/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

class ReplReceiver1 {
    val ok = "OK"
}

@Suppress("unused") // Used in snippets
class TestReplReceiver1 {
    fun checkReceiver(block: ReplReceiver1.() -> Any) = block(ReplReceiver1())
}
