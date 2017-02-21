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

import com.intellij.psi.PsiPackage
import org.jetbrains.kotlin.java.model.*
import org.jetbrains.kotlin.java.model.types.JePackageTypeMirror
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ElementVisitor
import javax.lang.model.element.PackageElement

class JePackageElement(psi: PsiPackage) : JeAbstractElement<PsiPackage>(psi), PackageElement, JeModifierListOwner, JeNoAnnotations {
    override fun getEnclosingElement() = null

    override fun getSimpleName() = JeName(psi.name)

    override fun getKind() = ElementKind.PACKAGE

    override fun <R : Any?, P : Any?> accept(v: ElementVisitor<R, P>, p: P) = v.visitPackage(this, p)

    override fun isUnnamed() = psi.name.isNullOrEmpty()

    override fun getQualifiedName() = JeName(psi.qualifiedName)

    override fun getEnclosedElements() = psi.classes.map(::JeTypeElement)

    override fun asType() = JePackageTypeMirror
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class.java != this::class.java) return false

        return psi == (other as JePackageElement).psi
    }

    override fun hashCode() = psi.hashCode()

    override fun toString() = psi.qualifiedName
}