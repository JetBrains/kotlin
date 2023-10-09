package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.jetbrains.org.objectweb.asm.tree.MethodNode

abstract class MethodPropertyChecker<T>(
    name: String,
    private val ignoreOnEquallyInvisibleMethods: Boolean = false,
) :
    PropertyChecker<T, MethodNode>("method.$name"),
    MethodChecker {

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        if (ignoreOnEquallyInvisibleMethods && areEquallyInvisible(method1, method2)) return
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