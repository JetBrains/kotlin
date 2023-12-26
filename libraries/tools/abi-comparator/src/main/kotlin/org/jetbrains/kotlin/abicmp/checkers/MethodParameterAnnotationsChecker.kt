/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.defects.*
import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.math.max
import kotlin.reflect.KProperty1

class MethodParameterAnnotationsChecker(
    private val parameterAnnotationsProperty: KProperty1<MethodNode, Array<List<AnnotationNode?>?>?>,
) : MethodChecker {

    override val name = "method.parameters.${parameterAnnotationsProperty.name}"

    val mismatchDefect = DefectType("${name}.mismatch", "Value parameter annotation mismatch", METHOD_A, VP_INDEX_A, VALUE1_A, VALUE2_A)
    val missing1Defect = DefectType("${name}.missing1", "Missing value parameter annotation in #1", METHOD_A, VP_INDEX_A, VALUE2_A)
    val missing2Defect = DefectType("${name}.missing2", "Missing value parameter annotation in #2", METHOD_A, VP_INDEX_A, VALUE1_A)

    override fun check(method1: MethodNode, method2: MethodNode, report: MethodReport) {
        val paramAnnsList1 = parameterAnnotationsProperty.get(method1)?.toList().orEmpty()
        val paramAnnsList2 = parameterAnnotationsProperty.get(method2)?.toList().orEmpty()
        for (i in 0 until max(paramAnnsList1.size, paramAnnsList2.size)) {
            val anns1 = paramAnnsList1.getOrElse(i) { emptyList() }.toAnnotations()
            val anns2 = paramAnnsList2.getOrElse(i) { emptyList() }.toAnnotations()
            val annDiff = compareAnnotations(anns1, anns2) ?: continue
            report.addValueParameterAnnotationDiffs(this, i, annDiff)
        }
    }
}