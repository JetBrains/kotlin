package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KProperty1

class MethodAnnotationsChecker(annotationsProperty: KProperty1<MethodNode, List<Any?>?>) :
    AnnotationsChecker<MethodNode>("method.${annotationsProperty.name}", annotationsProperty),
    MethodChecker {

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val anns1 = getAnnotations(method1)
        val anns2 = getAnnotations(method2).ignoreMissingNullabilityAnnotationsOnMethod(method1, method2, anns1)
        val annDiff = compareAnnotations(anns1, anns2) ?: return
        report.addAnnotationDiffs(this, annDiff)
    }
}