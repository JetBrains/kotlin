/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.NamedDiffEntry
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import kotlin.reflect.KProperty1

interface ClassChecker : Checker {
    fun check(class1: ClassNode, class2: ClassNode, report: ClassReport)
}

abstract class ClassPropertyChecker<T>(name: String) :
    PropertyChecker<T, ClassNode>("class.$name"),
    ClassChecker {

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val value1 = getProperty(class1)
        val value2 = getProperty(class2)
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

inline fun <T> classPropertyChecker(name: String, crossinline get: (ClassNode) -> T) =
    object : ClassPropertyChecker<T>(name) {
        override fun getProperty(node: ClassNode): T =
            get(node)
    }

fun <T> classPropertyChecker(classProperty: KProperty1<ClassNode, T>) =
    classPropertyChecker(classProperty.name) { classProperty.get(it) }

inline fun <T> classPropertyChecker(classProperty: KProperty1<ClassNode, T>, crossinline html: (T) -> String) =
    object : ClassPropertyChecker<T>(classProperty.name) {
        override fun getProperty(node: ClassNode): T =
            classProperty.get(node)

        override fun valueToHtml(value: T, other: T): String =
            html(value)
    }

fun <T> classPropertyChecker(name: String, classProperty: KProperty1<ClassNode, T>) =
    classPropertyChecker(name) { classProperty.get(it) }

class ClassAnnotationsChecker(annotationsProperty: KProperty1<ClassNode, List<Any?>?>) :
    AnnotationsChecker<ClassNode>("class.${annotationsProperty.name}", annotationsProperty),
    ClassChecker {

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val anns1 = getAnnotations(class1)
        val anns2 = getAnnotations(class2)
        report.addAnnotationDiffs(this, compareAnnotations(anns1, anns2) ?: return)
    }
}
