/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

// much stricter version of com.intellij.util.PathUtil.suggestFileName(java.lang.String)
// returns OS-neutral file name, which remains the same when generated on different OSes
fun suggestOsNeutralFileName(name: String) = name.asSequence().map { ch ->
    if (ch < 32.toChar() || ch in "<>:\"|?*.\\/;" || ch.isWhitespace()) '_' else ch
}.joinToString(separator = "")
