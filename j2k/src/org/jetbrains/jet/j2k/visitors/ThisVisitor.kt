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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.*
import org.jetbrains.annotations.Nullable
import java.util.LinkedHashSet

public open class ThisVisitor(): JavaRecursiveElementVisitor() {
    private val myResolvedConstructors = LinkedHashSet<PsiMethod>()

    public override fun visitReferenceExpression(expression: PsiReferenceExpression?): Unit {
        for (r : PsiReference? in expression?.getReferences()!!) {
            if (r?.getCanonicalText() == "this") {
                val res: PsiElement? = r?.resolve()
                if (res is PsiMethod && res.isConstructor()) {
                    myResolvedConstructors.add(res)
                }
            }
        }
    }

    public open fun getPrimaryConstructor(): PsiMethod? {
        if (myResolvedConstructors.size() > 0) {
            val first: PsiMethod = myResolvedConstructors.iterator().next()
            for (m in myResolvedConstructors)
                if (m.hashCode() != first.hashCode()) {
                    return null
                }

            return first
        }
        return null
    }
}
