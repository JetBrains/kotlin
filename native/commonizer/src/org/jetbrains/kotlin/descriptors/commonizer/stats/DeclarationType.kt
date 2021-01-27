/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

enum class DeclarationType(val alias: String) {
    TOP_LEVEL_CONST_VAL("TOP-LEVEL CONST-VAL"),
    TOP_LEVEL_VAL("TOP-LEVEL VAL"),
    TOP_LEVEL_FUN("TOP-LEVEL FUN"),
    TOP_LEVEL_CLASS("TOP-LEVEL CLASS"),
    CLASS_CONSTRUCTOR("CLASS_CONSTRUCTOR"),
    NESTED_VAL("NESTED VAL"),
    NESTED_FUN("NESTED FUN"),
    NESTED_CLASS("NESTED CLASS"),
    NESTED_INTERFACE("NESTED INTERFACE"),
    COMPANION_OBJECT("COMPANION_OBJECT"),
    TOP_LEVEL_INTERFACE("TOP-LEVEL INTERFACE"),
    TYPE_ALIAS("TYPE_ALIAS"),
    ENUM_ENTRY("ENUM_ENTRY"),
    ENUM_CLASS("ENUM_CLASS"),
    MODULE("MODULE")
}
