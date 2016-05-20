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

package org.jetbrains.kotlin.idea.spring.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.spring.model.highlighting.SpringJavaAutowiredMembersInspection
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.registerWithElementsUnwrapped
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitorVoid

class SpringKotlinAutowiredMembersInspection : AbstractKotlinInspection() {
    private val javaInspection by lazy { SpringJavaAutowiredMembersInspection() }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object: KtVisitorVoid() {
            override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                val lightClass = classOrObject.toLightClass() ?: return
                javaInspection.checkClass(lightClass, holder.manager, isOnTheFly)?.registerWithElementsUnwrapped(holder, isOnTheFly)
            }
        }
    }
}
