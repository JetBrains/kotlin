/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.java.model.internal

import com.intellij.psi.*
import com.intellij.psi.PsiModifier.*
import com.intellij.psi.impl.PsiSubstitutorImpl
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.kotlin.asJava.elements.KtLightAnnotation
import javax.lang.model.element.Modifier

private val HAS_DEFAULT by lazy {
    Modifier::class.java.declaredFields.any { it.name == "DEFAULT" }
}

private fun PsiModifierList.getJavaModifiers(): Set<Modifier> {
    fun MutableSet<Modifier>.check(modifier: String, javaModifier: Modifier) {
        if (hasModifierProperty(modifier)) this += javaModifier
    }

    return mutableSetOf<Modifier>().apply {
        check(PUBLIC, Modifier.PUBLIC)
        check(PROTECTED, Modifier.PROTECTED) 
        check(PRIVATE, Modifier.PRIVATE)
        check(STATIC, Modifier.STATIC)
        check(ABSTRACT, Modifier.ABSTRACT)
        check(FINAL, Modifier.FINAL)
        check(NATIVE, Modifier.NATIVE)
        check(SYNCHRONIZED, Modifier.SYNCHRONIZED)
        check(STRICTFP, Modifier.STRICTFP)
        check(TRANSIENT, Modifier.TRANSIENT)
        check(VOLATILE, Modifier.VOLATILE)
        
        if (HAS_DEFAULT) {
            check(DEFAULT, Modifier.DEFAULT)
        }
    }
}

internal fun PsiExpression.calcConstantValue(evaluator: PsiConstantEvaluationHelper? = null): Any? {
    return when (this) {
        is PsiLiteral -> value
        is KtLightAnnotation.LightExpressionValue<*> -> getConstantValue() ?: delegate.calcConstantValue(evaluator)
        is PsiExpression -> (evaluator ?: getConstantEvaluator(this)).computeConstantExpression(this)
        else -> null
    }
}

private fun getConstantEvaluator(expression: PsiExpression) = JavaPsiFacade.getInstance(expression.project).constantEvaluationHelper

internal val PsiModifierListOwner.isStatic: Boolean
    get() = hasModifierProperty(PsiModifier.STATIC)

internal val PsiModifierListOwner.isFinal: Boolean
    get() = hasModifierProperty(PsiModifier.FINAL)

fun PsiModifierListOwner.getJavaModifiers() = modifierList?.getJavaModifiers() ?: emptySet()

fun PsiModifierListOwner.getAnnotationsWithInherited(): List<PsiAnnotation> {
    val annotations = modifierList?.annotations?.toMutableList() ?: mutableListOf()

    if (this is PsiClass) {
        var superClass = superClass
        while (superClass != null) {
            superClass.modifierList?.annotations?.let { superClassAnnotations ->
                for (annotation in superClassAnnotations) {
                    // Do not add the inherited annotation from the superclass
                    // if the current class has an annotation with the same qualified name
                    if (!annotation.isInherited()
                        || annotations.any { it.qualifiedName == annotation.qualifiedName }) continue
                    annotations += annotation
                }
            }

            superClass = superClass.superClass
        }
    }

    return annotations
}

fun PsiClass.getTypeWithTypeParameters(): PsiClassType {
    val elementFactory = JavaPsiFacade.getElementFactory(project)
    val params = mutableMapOf<PsiTypeParameter, PsiType>()
    typeParameters.forEach { params.put(it, PsiTypesUtil.getClassType(it)) }
    return elementFactory.createType(this, PsiSubstitutorImpl.createSubstitutor(params))
}

private fun PsiAnnotation.isInherited(): Boolean {
    val annotationClass = nameReferenceElement?.resolve() as? PsiClass ?: return false
    val annotations = annotationClass.modifierList?.annotations ?: return false
    return annotations.any { it.qualifiedName == "java.lang.annotation.Inherited" }
}