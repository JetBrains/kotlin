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

import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifier.*
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner
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

internal val PsiModifierListOwner.isStatic: Boolean
    get() = hasModifierProperty(PsiModifier.STATIC)

internal val PsiModifierListOwner.isFinal: Boolean
    get() = hasModifierProperty(PsiModifier.FINAL)

fun PsiModifierListOwner.getJavaModifiers() = modifierList?.getJavaModifiers() ?: emptySet()