/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.group

import org.objectweb.asm.Label

data class GroupInfo(
    val key: Int?,
    val type: GroupType,
    var line: Int,
) {
    internal var incompleteLabels = mutableListOf<Label>()
    internal var markers = mutableListOf<Int>()
}

enum class GroupType {
    Root,
    RestartGroup,
    ReplaceGroup,
}