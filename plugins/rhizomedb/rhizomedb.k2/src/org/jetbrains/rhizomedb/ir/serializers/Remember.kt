/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.ir.serializers

import org.jetbrains.kotlin.name.Name

@Suppress("UNCHECKED_CAST")
fun <T> remember(block: () -> T): T = rememberMap.getOrPut(block) { block() } as T

val String.name: Name
    get() = Name.identifier(this)

private val rememberMap = mutableMapOf<Any, Any?>()