/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.group

class GroupInfo(
    val key: Int?,
    val type: GroupType,
    var line: Int,
)

enum class GroupType {
    Root,
    RestartGroup,
    ReplaceGroup,
}