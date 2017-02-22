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

package org.jetbrains.kotlin.java.model.elements

import com.intellij.psi.PsiClassInitializer
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.internal.isStatic
import org.jetbrains.kotlin.java.model.types.JeClassInitializerExecutableTypeMirror
import org.jetbrains.kotlin.java.model.types.JeNoneType
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

class JeClassInitializerExecutableElement(
        psi: PsiClassInitializer
) : JeAbstractElement<PsiClassInitializer>(psi), ExecutableElement, JeNoAnnotations, JeModifierListOwner {
    val isStaticInitializer = psi.isStatic

    override fun getEnclosingElement() = psi.containingClass?.let(::JeTypeElement)

    override fun getSimpleName() = if (isStaticInitializer) JeName.CLINIT else JeName.EMPTY

    override fun getThrownTypes() = emptyList<TypeMirror>()

    override fun getTypeParameters() = emptyList<TypeParameterElement>()

    override fun getParameters() = emptyList<VariableElement>()

    override fun getDefaultValue() = null

    override fun getReturnType() = JeNoneType

    override fun getReceiverType() = JeNoneType

    override fun isVarArgs() = false

    override fun isDefault() = false

    override fun getKind() = if (isStaticInitializer) ElementKind.STATIC_INIT else ElementKind.INSTANCE_INIT

    override fun asType() = JeClassInitializerExecutableTypeMirror(psi)

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitExecutable(this, p)

    override fun getEnclosedElements() = emptyList<Element>()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false

        return psi == (other as JeClassInitializerExecutableElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.containingClass?.qualifiedName
            ?.let { it + if (isStaticInitializer) ".<clinit>" else ".<instinit>" } ?: "<unnamed>"
}