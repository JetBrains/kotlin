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

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.JavaRecursiveElementVisitor
import java.util.LinkedHashSet
import com.intellij.psi.PsiReference

fun isConstructorPrimary(constructor: PsiMethod): Boolean {
    val parent = constructor.getParent()
    if (parent is PsiClass) {
        if (parent.getConstructors().size == 1) {
            return true
        }
        else {
            val primary = getPrimaryConstructorForThisCase(parent)
            //TODO: I do not understand code below
            if (primary != null && primary.hashCode() == constructor.hashCode()) {
                return true
            }
        }
    }
    return false
}

fun getPrimaryConstructorForThisCase(psiClass: PsiClass): PsiMethod? {
    class FindPrimaryConstructorVisitor() : JavaRecursiveElementVisitor() {
        private val resolvedConstructors = LinkedHashSet<PsiMethod>()

        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            expression.getReferences()
                    .filter { it.getCanonicalText() == "this" }
                    .map { it.resolve() }
                    .filterIsInstance(javaClass<PsiMethod>())
                    .filterTo(resolvedConstructors) { it.isConstructor() }
        }

        val result: PsiMethod?
            get() {
                if (resolvedConstructors.isEmpty()) return null
                //TODO: I do not understand code below
                val first = resolvedConstructors.first()
                return if (resolvedConstructors.all { it.hashCode() == first.hashCode() }) first else null
            }
    }

    val visitor = FindPrimaryConstructorVisitor()
    psiClass.accept(visitor)
    return visitor.result
}

fun isSuperConstructorRef(ref: PsiReference): Boolean {
    if (ref.getCanonicalText().equals("super")) {
        val baseConstructor = ref.resolve()
        if (baseConstructor is PsiMethod && baseConstructor.isConstructor()) {
            return true
        }
    }

    return false
}
