/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class NameMatcher(val name: MangledName) {
    companion object {
        inline fun match(name: MangledName, action: NameMatcher.() -> Nothing): Nothing {
            NameMatcher(name).action()
        }
    }

    private val scopedName = name as? ScopedKotlinName
    private val packageName = (scopedName?.scope as? PackagePrefixScope)?.packageName
    private val className = (scopedName?.scope as? ClassScope)?.className

    inline fun ifPackageName(vararg segments: String, action: NameMatcher.() -> Unit) {
        if (packageName == FqName.fromSegments(segments.toList()))
            this.action()
    }

    inline fun ifClassName(vararg segments: String, action: NameMatcher.() -> Unit) {
        if (className == ClassKotlinName(segments.toList()))
            this.action()
    }

    inline fun ifFunctionName(name: String, action: NameMatcher.() -> Unit) {
        if (scopedName?.name == FunctionKotlinName(Name.identifier(name)))
            this.action()
    }

    inline fun ifMemberName(name: String, action: NameMatcher.() -> Unit) {
        if (scopedName?.name == MemberKotlinName(Name.identifier(name)))
            this.action()
    }

    inline fun ifInCollectionsPkg(action: NameMatcher.() -> Unit) {
        ifPackageName("kotlin", "collections") { action() }
    }

    inline fun ifIsCollectionInterface(action: NameMatcher.() -> Unit) {
        ifPackageName("kotlin", "collections") {
            ifClassName("Collection") {
                action()
            }
        }
    }
}