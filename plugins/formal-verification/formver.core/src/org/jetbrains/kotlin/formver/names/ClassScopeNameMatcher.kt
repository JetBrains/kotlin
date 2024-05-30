/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal sealed class NameMatcher(val name: MangledName) {

    companion object {
        inline fun matchClassScope(name: MangledName, action: ClassScopeNameMatcher.() -> Nothing): Nothing {
            ClassScopeNameMatcher(name).action()
        }

        inline fun matchGlobalScope(name: MangledName, action: GlobalScopeNameMatcher.() -> Nothing): Nothing {
            GlobalScopeNameMatcher(name).action()
        }
    }

    protected val scopedName = name as? ScopedKotlinName
    protected val packageName = (scopedName?.scope as? PackagePrefixScope)?.packageName
    protected abstract val className: ClassKotlinName?

    inline fun ifPackageName(vararg segments: String, action: NameMatcher.() -> Unit) {
        if (packageName == FqName.fromSegments(segments.toList()))
            this.action()
    }

    inline fun ifInCollectionsPkg(action: NameMatcher.() -> Unit) {
        ifPackageName("kotlin", "collections") { this.action() }
    }

    inline fun ifClassName(vararg segments: String, action: NameMatcher.() -> Unit) {
        if (className == ClassKotlinName(segments.toList()))
            this.action()
    }

    inline fun ifIsCollectionInterface(action: NameMatcher.() -> Unit) {
        ifInCollectionsPkg {
            ifClassName("Collection") {
                this.action()
            }
        }
    }

}

internal class ClassScopeNameMatcher(name: MangledName) : NameMatcher(name) {

    override val className = (scopedName?.scope as? ClassScope)?.className

    inline fun ifNoReceiver(action: NameMatcher.() -> Unit) {
        if (className == null)
            action()
    }

    inline fun ifFunctionName(name: String, action: ClassScopeNameMatcher.() -> Unit) {
        if (scopedName?.name == FunctionKotlinName(Name.identifier(name)))
            this.action()
    }

    inline fun ifBackingFieldName(name: String, action: ClassScopeNameMatcher.() -> Unit) {
        if (scopedName?.name == BackingFieldKotlinName(Name.identifier(name)))
            this.action()
    }
}

internal class GlobalScopeNameMatcher(name: MangledName) : NameMatcher(name) {

    override val className = scopedName?.name as ClassKotlinName?
}