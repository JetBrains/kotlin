package org.jetbrains.dokka.base.translators.psi

import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.dokka.base.translators.firstNotNullOfOrNull
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.propertyNameByGetMethodName
import org.jetbrains.kotlin.load.java.propertyNamesBySetMethodName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils


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
    return PsiFunctionsHolder(regularFunctions, accessors)
}

internal fun PsiMethod.getPossiblePropertyNamesForFunction(): List<String> {
    val jvmName = getAnnotation(DescriptorUtils.JVM_NAME.asString())?.findAttributeValue("name")?.text
    return jvmName?.let { listOf(jvmName) }
        ?: when {
            JvmAbi.isGetterName(name) -> listOfNotNull(
                propertyNameByGetMethodName(Name.identifier(name))?.asString()
            )
            JvmAbi.isSetterName(name) -> {
                propertyNamesBySetMethodName(Name.identifier(name)).map { it.asString() }
            }
            else -> listOf()
        }
}

internal fun PsiMethod.isAccessorFor(field: PsiField): Boolean {
    return this.isGetterFor(field) || this.isSetterFor(field)
}

internal fun PsiMethod.isGetterFor(field: PsiField): Boolean {
    return this.returnType == field.type && !this.hasParameters()
}

internal fun PsiMethod.isSetterFor(field: PsiField): Boolean {
    return parameterList.getParameter(0)?.type == field.type
}
