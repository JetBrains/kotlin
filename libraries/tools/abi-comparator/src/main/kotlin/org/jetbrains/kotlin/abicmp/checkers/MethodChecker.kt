/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KProperty1

const val ignoreMissingNullabilityAnnotationsOnInvisibleMethods = true

interface MethodChecker : Checker {
    fun check(method1: MethodNode, method2: MethodNode, report: MethodReport)
}

inline fun <T> methodPropertyChecker(name: String, crossinline get: (MethodNode) -> T) =
    object : MethodPropertyChecker<T>(name) {
        override fun getProperty(node: MethodNode): T =
            get(node)
    }

fun <T> methodPropertyChecker(methodProperty: KProperty1<MethodNode, T>) =
    methodPropertyChecker(methodProperty.name) { methodProperty.get(it) }

inline fun <T> methodPropertyChecker(methodProperty: KProperty1<MethodNode, T>, crossinline html: (T) -> String) =
    object : MethodPropertyChecker<T>(methodProperty.name) {
        override fun getProperty(node: MethodNode): T =
            methodProperty.get(node)

        override fun valueToHtml(value: T, other: T): String =
            html(value)
    }

fun <T> methodPropertyChecker(name: String, methodProperty: KProperty1<MethodNode, T>) =
    methodPropertyChecker(name) { methodProperty.get(it) }