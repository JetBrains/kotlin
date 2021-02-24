/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

fun <T : Any> T?.toSetOrEmpty(): Set<T> =
    if (this == null) emptySet() else setOf(this)