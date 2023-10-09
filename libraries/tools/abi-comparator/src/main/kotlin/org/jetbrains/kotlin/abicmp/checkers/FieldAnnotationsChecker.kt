package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.isPrivate
import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import kotlin.reflect.KProperty1

class FieldAnnotationsChecker(
    annotationsProperty: KProperty1<FieldNode, List<Any?>?>,
    val ignoreNullabilityAnnotationsInIrBuild: Boolean = false,
) :
    AnnotationsChecker<FieldNode>("field.${annotationsProperty.name}", annotationsProperty),
    FieldChecker {

    override fun check(field1: FieldNode, field2: FieldNode, report: FieldReport) {
        val anns1 = getAnnotations(field1)
        val anns2 = getAnnotations(field2)
        val anns2filtered =
            if (ignoreNullabilityAnnotationsInIrBuild &&
                field2.access.isPrivate() &&
                anns1.none { it.isNullabilityAnnotation() }
            )
                anns2.filterNot { it.isNullabilityAnnotation() }
            else
                anns2
        val annDiff = compareAnnotations(anns1, anns2filtered) ?: return
        report.addAnnotationDiffs(this, annDiff)
    }
}