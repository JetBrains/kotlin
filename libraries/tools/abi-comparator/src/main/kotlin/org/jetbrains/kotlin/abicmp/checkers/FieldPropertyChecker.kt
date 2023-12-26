/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.jetbrains.org.objectweb.asm.tree.FieldNode

abstract class FieldPropertyChecker<T>(name: String) :
    PropertyChecker<T, FieldNode>("field.$name"),
    FieldChecker {

    override fun check(field1: FieldNode, field2: FieldNode, report: FieldReport) {
        val value1 = getProperty(field1)
        val value2 = getProperty(field2)
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