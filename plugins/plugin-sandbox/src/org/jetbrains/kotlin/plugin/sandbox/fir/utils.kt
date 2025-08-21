/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox.fir

import org.jetbrains.kotlin.name.FqName

val SANDBOX_ANNOTATIONS_PACKAGE = FqName("org.jetbrains.kotlin.plugin.sandbox")
fun String.fqn(): FqName = FqName("${SANDBOX_ANNOTATIONS_PACKAGE.asString()}.$this")
