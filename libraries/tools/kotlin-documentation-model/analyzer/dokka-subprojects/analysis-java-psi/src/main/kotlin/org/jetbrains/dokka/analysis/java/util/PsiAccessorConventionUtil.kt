/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.dokka.analysis.java.getVisibility
import org.jetbrains.dokka.model.JavaVisibility
import org.jetbrains.dokka.model.KotlinVisibility
import org.jetbrains.dokka.model.Visibility


internal data class PsiFunctionsHolder(
    val regularFunctions: List<PsiMethod>,
    val accessors: Map<PsiField, List<PsiMethod>>
)

internal fun splitFunctionsAndAccessors(fields: Array<PsiField>, methods: Array<PsiMethod>): PsiFunctionsHolder {
    val fieldsByName = fields.associateBy { it.name }
    val regularFunctions = mutableListOf<PsiMethod>()
    val accessors = mutableMapOf<PsiField, MutableList<PsiMethod>>()
    methods.forEach { method ->
        val possiblePropertyNamesForFunction = method.getPossiblePropertyNamesForFunction()
        val field = possiblePropertyNamesForFunction.firstNotNullOfOrNull { fieldsByName[it] }
        if (field != null && method.isAccessorFor(field)) {
            accessors.getOrPut(field, ::mutableListOf).add(method)
        } else {
            regularFunctions.add(method)
        }
    }

    val accessorLookalikes = removeNonAccessorsReturning(accessors)
    regularFunctions.addAll(accessorLookalikes)

    return PsiFunctionsHolder(regularFunctions, accessors)
}

private fun PsiMethod.getPossiblePropertyNamesForFunction(): List<String> {
    val jvmName = getAnnotation("kotlin.jvm.JvmName")?.findAttributeValue("name")?.text
    if (jvmName != null) return listOf(jvmName)

    return when {
            isGetterName(name) -> listOfNotNull(
                propertyNameByGetMethodName(name)
            )
            isSetterName(name) -> {
                propertyNamesBySetMethodName(name)
            }
            else -> listOf()
        }
}

private fun isGetterName(name: String): Boolean {
    return name.startsWith("get") || name.startsWith("is")
}

private fun isSetterName(name: String): Boolean {
    return name.startsWith("set")
}

/**
 * If a field has no getter, it's not accessible as a property from Kotlin's perspective,
 * but it still might have a setter. In this case, this "setter" should be just a regular function
 */
private fun removeNonAccessorsReturning(
    fieldAccessors: MutableMap<PsiField, MutableList<PsiMethod>>
): List<PsiMethod> {
    val nonAccessors = mutableListOf<PsiMethod>()
    fieldAccessors.entries.removeIf { (field, methods) ->
        if (methods.size == 1 && methods[0].isSetterFor(field)) {
            nonAccessors.add(methods[0])
            true
        } else {
            false
        }
    }
    return nonAccessors
}

internal fun PsiMethod.isAccessorFor(field: PsiField): Boolean {
    return (this.isGetterFor(field) || this.isSetterFor(field))
            && !field.getVisibility().isPublicAPI()
            && this.getVisibility().isPublicAPI()
}

internal fun PsiMethod.isGetterFor(field: PsiField): Boolean {
    return this.returnType == field.type && !this.hasParameters()
}

internal fun PsiMethod.isSetterFor(field: PsiField): Boolean {
    return parameterList.getParameter(0)?.type == field.type && parameterList.getParametersCount() == 1
}

private fun Visibility.isPublicAPI() = when(this) {
    KotlinVisibility.Public,
    KotlinVisibility.Protected,
    JavaVisibility.Public,
    JavaVisibility.Protected -> true
    else -> false
}
