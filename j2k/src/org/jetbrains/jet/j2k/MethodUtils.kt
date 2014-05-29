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
import com.intellij.psi.PsiModifier
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT

/*
* Overrides of methods from object should not be marked as overrides in Kotlin unless the class itself has java ancestors
* */
fun Converter.isOverride(method: PsiMethod): Boolean {
    val superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures()
    val overridesMethodNotFromObject = superSignatures any {
        it.getMethod().getContainingClass()?.getQualifiedName() != JAVA_LANG_OBJECT
    }

    if (overridesMethodNotFromObject) {
        return true
    }

    val overridesMethodFromObject = superSignatures any {
        it.getMethod().getContainingClass()?.getQualifiedName() == JAVA_LANG_OBJECT
    }

    if (overridesMethodFromObject) {
        val containing = method.getContainingClass()
        if (containing != null) {
            val hasOtherJavaSuperclasses = containing.getSuperTypes() any {
                val canonicalText = it.getCanonicalText()
                //TODO: correctly check for kotlin class
                canonicalText != JAVA_LANG_OBJECT && !getClassIdentifiers().contains(canonicalText)
            }
            if (hasOtherJavaSuperclasses) {
                return true
            }
        }
    }

    return false
}

fun isNotOpenMethod(method: PsiMethod): Boolean {
    val parent = method.getParent()
    if (parent is PsiClass) {
        val parentModifierList = parent.getModifierList()
        if ((parentModifierList != null && parentModifierList.hasExplicitModifier(PsiModifier.FINAL)) || parent.isEnum()) {
            return true
        }
    }
    return false
}

fun directlyOverridesMethodFromObject(method: PsiMethod): Boolean {
    var superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures()
    if (superSignatures.size() == 1) {
        val qualifiedName = superSignatures.single().getMethod().getContainingClass()?.getQualifiedName()
        return qualifiedName == JAVA_LANG_OBJECT
    }
    return false
}