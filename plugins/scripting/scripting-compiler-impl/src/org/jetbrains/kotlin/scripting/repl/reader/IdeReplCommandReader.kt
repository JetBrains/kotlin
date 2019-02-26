/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.reader

import org.jetbrains.kotlin.scripting.repl.ReplFromTerminal

class IdeReplCommandReader : ReplCommandReader {
    override fun readLine(next: ReplFromTerminal.WhatNextAfterOneLine) = readLine()
    override fun flushHistory() = Unit
}
