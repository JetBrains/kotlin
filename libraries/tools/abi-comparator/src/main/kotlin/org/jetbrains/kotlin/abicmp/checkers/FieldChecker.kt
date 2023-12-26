/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import kotlin.reflect.KProperty1

interface FieldChecker : Checker {
    fun check(field1: FieldNode, field2: FieldNode, report: FieldReport)
}

inline fun <T> fieldPropertyChecker(name: String, crossinline get: (FieldNode) -> T) =
    object : FieldPropertyChecker<T>(name) {
        override fun getProperty(node: FieldNode): T =
            get(node)
    }

fun <T> fieldPropertyChecker(fieldProperty: KProperty1<FieldNode, T>) =
    fieldPropertyChecker(fieldProperty.name) { fieldProperty.get(it) }

fun <T> fieldPropertyChecker(name: String, fieldProperty: KProperty1<FieldNode, T>) =
    fieldPropertyChecker(name) { fieldProperty.get(it) }

inline fun <T> fieldPropertyChecker(fieldProperty: KProperty1<FieldNode, T>, crossinline html: (T) -> String) =
    object : FieldPropertyChecker<T>(fieldProperty.name) {
        override fun getProperty(node: FieldNode): T =
            fieldProperty.get(node)

        override fun valueToHtml(value: T, other: T): String =
            html(value)
    }

