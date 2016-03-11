/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies

class AndroidVariant(val name: String, val resDirectories: List<String>) {
    val packageName: String = name
    val isMainVariant: Boolean
        get() = name == "main"

    companion object {
        fun createMainVariant(resDirectories: List<String>) = AndroidVariant("main", resDirectories)
    }
}

class AndroidModule(val applicationPackage: String, val variants: List<AndroidVariant>) {
    override fun equals(other: Any?) = other is AndroidModule && applicationPackage == other.applicationPackage
    override fun hashCode() = applicationPackage.hashCode()
}

sealed class AndroidResource(val id: String, val sourceElement: PsiElement?) {
    open fun sameClass(other: AndroidResource): Boolean = false

    class Widget(
            id: String,
            val xmlType: String,
            sourceElement: PsiElement?
    ) : AndroidResource(id, sourceElement) {
        override fun sameClass(other: AndroidResource) = other is Widget
    }

    class Fragment(id: String, sourceElement: PsiElement?) : AndroidResource(id, sourceElement) {
        override fun sameClass(other: AndroidResource) = other is Fragment
    }
}

fun <T> cachedValue(project: Project, result: () -> CachedValueProvider.Result<T>): CachedValue<T> {
    return CachedValuesManager.getManager(project).createCachedValue(result, false)
}

class ResolvedWidget(val widget: AndroidResource.Widget, val viewClassDescriptor: ClassDescriptor?) {
    val isErrorType: Boolean
        get() = viewClassDescriptor == null

    val errorType: String?
        get() = if (isErrorType) widget.xmlType else null
}

fun AndroidResource.Widget.resolve(module: ModuleDescriptor): ResolvedWidget {
    fun resolve(fqName: String) = module.findClassAcrossModuleDependencies(ClassId.topLevel(FqName(fqName)))

    if ('.' in xmlType) {
        return ResolvedWidget(this, resolve(xmlType))
    }

    for (packageName in AndroidConst.FQNAME_RESOLVE_PACKAGES) {
        val classDescriptor = resolve("$packageName.$xmlType")
        if (classDescriptor != null) {
            return ResolvedWidget(this, classDescriptor)
        }
    }

    return ResolvedWidget(this, null)
}