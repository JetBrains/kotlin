/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.tree.conveersionCache

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.j2k.tree.JKClass
import org.jetbrains.kotlin.j2k.tree.JKField
import org.jetbrains.kotlin.j2k.tree.JKJavaField
import org.jetbrains.kotlin.j2k.tree.JKMethod
import org.jetbrains.kotlin.j2k.tree.impl.JKNameIdentifierImpl

interface ReferenceTargetProvider {
    fun resolveClassReference(identifier: String): JKClass
    fun resolveClassReference(clazz: PsiClass): JKClass
    fun putUniverseClass(clazz: JKClass)
    fun putMultiverseClass(clazz: JKMultiverseClass)
    fun resolveMethodReference(clazz: JKClass, method: PsiMethod): JKMethod {
        return clazz.declarations.asSequence().filter { it is JKMethod && it.name.name == method.name }.firstOrNull() as JKMethod?
                ?: JKMultiverseMethod(JKNameIdentifierImpl(method.name)).also { clazz.declarations += it }
    }

    fun resolveFieldReference(clazz: JKClass, field: PsiField): JKField {
        return clazz.declarations.asSequence().filter { it is JKJavaField && it.name.name == field.name }.firstOrNull() as JKField?
                ?: JKMultiverseField(JKNameIdentifierImpl(field.name)).also { clazz.declarations += it }
    }
}