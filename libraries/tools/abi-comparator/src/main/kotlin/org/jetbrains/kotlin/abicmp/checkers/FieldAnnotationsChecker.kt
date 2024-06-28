/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.FieldReport
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import kotlin.reflect.KProperty1

class FieldAnnotationsChecker(annotationsProperty: KProperty1<FieldNode, List<Any?>?>) :
    AnnotationsChecker<FieldNode>("field.${annotationsProperty.name}", annotationsProperty),
    FieldChecker {

    override fun check(field1: FieldNode, field2: FieldNode, report: FieldReport) {
        val anns1 = getAnnotations(field1)
        val anns2 = getAnnotations(field2)
        val annDiff = compareAnnotations(anns1, anns2) ?: return
        report.addAnnotationDiffs(this, annDiff)
    }
}