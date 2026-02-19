/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.utils

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.sir.util.isValidSwiftIdentifier

internal fun String.rootPackageToFqn(): FqName? {
    if (!FqNameUnsafe.isValid(this)) return null
    if (!FqName(this).pathSegments().all { segment -> segment.toString().isValidSwiftIdentifier }) return null
    return FqName(this)
}