/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.jetbrains.org.objectweb.asm.tree.MethodNode

abstract class MethodPropertyChecker<T>(name: String) :
    PropertyChecker<T, MethodNode>("method.$name"),
    MethodChecker {

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val value1 = getProperty(method1)
        val value2 = getProperty(method2)
        if (!areEqual(value1, value2)) {
            report.addPropertyDiff(
                defectType,
                NamedDiffEntry(
                    name,
                    valueToHtml(value1, value2),
                    valueToHtml(value2, value1)
                )
            )
        }
    }
}