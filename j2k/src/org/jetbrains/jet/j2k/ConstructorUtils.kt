/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import com.intellij.psi.*
import java.util.HashMap

fun PsiMethod.isPrimaryConstructor(): Boolean {
    if (!isConstructor()) return false
    val parent = getParent()
    if (parent !is PsiClass) return false
    return parent.getPrimaryConstructor() == this
}

fun PsiClass.getPrimaryConstructor(): PsiMethod? {
    val constructors = getConstructors()
    when (constructors.size) {
        0 -> return null

        1 -> return constructors.single()

        else -> {
            val toTargetConstructorMap = HashMap<PsiMethod, PsiMethod>()
            for (constructor in constructors) {
                val firstStatement = constructor.getBody()?.getStatements()?.firstOrNull()
                val refExpr = ((firstStatement as? PsiExpressionStatement)
                        ?.getExpression() as? PsiMethodCallExpression)
                            ?.getMethodExpression()
                if (refExpr != null && refExpr.getCanonicalText() == "this") {
                    val target = refExpr.resolve() as? PsiMethod
                    if (target != null && target.isConstructor()) {
                        val finalTarget = toTargetConstructorMap[target] ?: target!!/*TODO: see KT-5335*/
                        toTargetConstructorMap[constructor] = finalTarget
                        for (entry in toTargetConstructorMap.entrySet()) {
                            if (entry.getValue() == constructor) {
                                entry.setValue(finalTarget)
                            }
                        }
                    }
                }
            }

            val candidates = constructors.filter { it !in toTargetConstructorMap }
            if (candidates.size != 1) return null // there should be only one constructor which does not call other constructor
            val candidate = candidates.single()
            return if (toTargetConstructorMap.values().all { it == candidate } /* all other constructors call our candidate (directly or indirectly)*/)
                candidate
            else
                null
        }
    }
}

fun PsiElement.getContainingMethod(): PsiMethod? {
    var context = getContext()
    while (context != null) {
        val _context = context!!
        if (_context is PsiMethod) return _context
        context = _context.getContext()
    }
    return null
}

fun PsiElement.getContainingConstructor(): PsiMethod? {
    val method = getContainingMethod()
    return if (method?.isConstructor() == true) method else null
}

fun PsiMethodCallExpression.isSuperConstructorCall(): Boolean {
    val ref = getMethodExpression()
    if (ref.getCanonicalText() == "super") {
        return ref.resolve()?.isConstructor() ?: false
    }
    return false
}

fun PsiElement.isConstructor(): Boolean = this is PsiMethod && this.isConstructor()
