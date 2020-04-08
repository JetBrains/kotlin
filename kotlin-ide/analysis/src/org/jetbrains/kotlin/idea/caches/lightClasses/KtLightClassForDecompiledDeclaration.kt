/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.lightClasses

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.compiled.ClsClassImpl
import org.jetbrains.kotlin.asJava.classes.KtLightClassBase
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject

class KtLightClassForDecompiledDeclaration(
    override val clsDelegate: ClsClassImpl,
    override val kotlinOrigin: KtClassOrObject?,
    private val file: KtClsFile
) : KtLightClassBase(clsDelegate.manager) {
    val fqName = kotlinOrigin?.fqName ?: FqName(clsDelegate.qualifiedName.orEmpty())

    override fun copy() = this

    override fun getOwnInnerClasses(): List<PsiClass> {
        val nestedClasses = kotlinOrigin?.declarations?.filterIsInstance<KtClassOrObject>() ?: emptyList()
        return clsDelegate.ownInnerClasses.map { innerClsClass ->
            KtLightClassForDecompiledDeclaration(
                innerClsClass as ClsClassImpl,
                nestedClasses.firstOrNull { innerClsClass.name == it.name }, file
            )
        }
    }

    override fun getOwnFields(): List<PsiField> {
        return clsDelegate.ownFields.map { KtLightFieldImpl.create(LightMemberOriginForCompiledField(it, file), it, this) }
    }

    override fun getOwnMethods(): List<PsiMethod> {
        return clsDelegate.ownMethods.map { KtLightMethodImpl.create(it, LightMemberOriginForCompiledMethod(it, file), this) }
    }

    override fun getNavigationElement() = kotlinOrigin?.navigationElement ?: file

    override fun getParent() = clsDelegate.parent

    override fun equals(other: Any?): Boolean =
        other is KtLightClassForDecompiledDeclaration &&
                fqName == other.fqName

    override fun hashCode(): Int =
        fqName.hashCode()

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.BINARY
}

