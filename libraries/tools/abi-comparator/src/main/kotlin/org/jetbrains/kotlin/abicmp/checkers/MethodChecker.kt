package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.isBridge
import org.jetbrains.kotlin.abicmp.isPrivate
import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.reports.MethodReport
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import kotlin.reflect.KProperty1

const val ignoreMissingNullabilityAnnotationsOnInvisibleMethods = true

interface MethodChecker : Checker {
    fun check(method1: MethodNode, method2: MethodNode, report: MethodReport)
}

inline fun <T> methodPropertyChecker(
    name: String,
    ignoreOnEquallyInvisibleMethods: Boolean = false,
    crossinline get: (MethodNode) -> T,
) =
    object : MethodPropertyChecker<T>(name, ignoreOnEquallyInvisibleMethods) {
        override fun getProperty(node: MethodNode): T =
            get(node)
    }

fun <T> methodPropertyChecker(
    methodProperty: KProperty1<MethodNode, T>,
    ignoreOnEquallyInvisibleMethods: Boolean = false,
) =
    methodPropertyChecker(methodProperty.name, ignoreOnEquallyInvisibleMethods) { methodProperty.get(it) }

inline fun <T> methodPropertyChecker(methodProperty: KProperty1<MethodNode, T>, crossinline html: (T) -> String) =
    object : MethodPropertyChecker<T>(methodProperty.name) {
        override fun getProperty(node: MethodNode): T =
            methodProperty.get(node)

        override fun valueToHtml(value: T, other: T): String =
            html(value)
    }

fun <T> methodPropertyChecker(name: String, methodProperty: KProperty1<MethodNode, T>) =
    methodPropertyChecker(name) { methodProperty.get(it) }

fun areEquallyInvisible(method1: MethodNode, method2: MethodNode) =
    method1.access.isPrivate() && method2.access.isPrivate() ||
            method1.access.isSynthetic() && method2.access.isSynthetic() ||
            method1.access.isBridge() && method2.access.isBridge() ||
            method1.name.contains('-') && method2.name.contains('-')

fun List<AnnotationEntry>.ignoreMissingNullabilityAnnotationsOnMethod(
    method1: MethodNode,
    method2: MethodNode,
    anns1: List<AnnotationEntry>,
) =
    if (ignoreMissingNullabilityAnnotationsOnInvisibleMethods &&
        areEquallyInvisible(method1, method2) &&
        anns1.none { it.isNullabilityAnnotation() }
    )
        filterNot { it.isNullabilityAnnotation() }
    else
        this

