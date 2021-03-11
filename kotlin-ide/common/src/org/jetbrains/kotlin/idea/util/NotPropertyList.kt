/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util


import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor

fun FunctionDescriptor.shouldNotConvertToProperty(notProperties: Set<FqNameUnsafe>): Boolean {
    if (fqNameUnsafe in notProperties) return true
    return this.overriddenTreeUniqueAsSequence(false).any { fqNameUnsafe in notProperties }
}

fun SyntheticJavaPropertyDescriptor.suppressedByNotPropertyList(set: Set<FqNameUnsafe>) =
    getMethod.shouldNotConvertToProperty(set) || setMethod?.shouldNotConvertToProperty(set) ?: false