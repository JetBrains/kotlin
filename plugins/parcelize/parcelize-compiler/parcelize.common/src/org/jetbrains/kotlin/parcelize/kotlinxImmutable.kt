/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

/**
 * Required because :kotlin-compiler-embeddable performs package relocation
 * If there's a "kotlinx.collections.immutable" string literal in bytecode
 * it becomes "org.jetbrains.kotlin.kotlinx.collections.immutable" thus
 * breaking target project class name matching
 */
fun kotlinxImmutable(name: String? = null): String {
    return listOfNotNull("kotlinx", "collections", "immutable", name).joinToString(".")
}