/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package androidx.compose.compiler.mapping.group

data class GroupInfo(
    var key: Int?,
    val type: GroupType,
    val line: Int,
)

enum class GroupType {
    Root,
    RestartGroup,
    ReplaceGroup
}