/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.stats

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.DescriptorUtils

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
    MODULE("MODULE"),
    UNKNOWN("UNKNOWN");

    companion object {
        val DeclarationDescriptor.declarationType: DeclarationType
            get() = when (this) {
                is ClassDescriptor -> {
                    if (isCompanionObject) {
                        COMPANION_OBJECT
                    } else when (kind) {
                        ClassKind.ENUM_CLASS -> ENUM_CLASS
                        ClassKind.ENUM_ENTRY -> ENUM_ENTRY
                        ClassKind.INTERFACE -> if (DescriptorUtils.isTopLevelDeclaration(this)) TOP_LEVEL_INTERFACE else NESTED_INTERFACE
                        else -> if (DescriptorUtils.isTopLevelDeclaration(this)) TOP_LEVEL_CLASS else NESTED_CLASS
                    }
                }
                is TypeAliasDescriptor -> TYPE_ALIAS
                is ClassConstructorDescriptor -> CLASS_CONSTRUCTOR
                is FunctionDescriptor -> if (DescriptorUtils.isTopLevelDeclaration(this)) TOP_LEVEL_FUN else NESTED_FUN
                is PropertyDescriptor -> if (DescriptorUtils.isTopLevelDeclaration(this)) {
                    if (isConst) TOP_LEVEL_CONST_VAL else TOP_LEVEL_VAL
                } else NESTED_VAL
                is ModuleDescriptor -> MODULE
                else -> UNKNOWN
            }
    }
}
