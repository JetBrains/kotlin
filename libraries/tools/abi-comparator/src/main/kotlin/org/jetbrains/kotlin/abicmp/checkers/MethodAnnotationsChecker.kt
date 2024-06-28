/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KProperty1

class MethodAnnotationsChecker(annotationsProperty: KProperty1<MethodNode, List<Any?>?>) :
    AnnotationsChecker<MethodNode>("method.${annotationsProperty.name}", annotationsProperty),
    MethodChecker {

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val anns1 = getAnnotations(method1)
        val anns2 = getAnnotations(method2)
        val annDiff = compareAnnotations(anns1, anns2) ?: return
        report.addAnnotationDiffs(this, annDiff)
    }
}