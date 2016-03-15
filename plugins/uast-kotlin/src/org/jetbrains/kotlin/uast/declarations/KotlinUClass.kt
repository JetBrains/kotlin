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

package org.jetbrains.kotlin.uast

import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.state.IncompatibleClassTracker
import org.jetbrains.kotlin.codegen.state.JetTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.uast.*
import org.jetbrains.uast.kinds.UastClassKind
import org.jetbrains.uast.psi.PsiElementBacked

class KotlinUClass(
        override val psi: KtClassOrObject,
        override val parent: UElement
) : UClass, PsiElementBacked {
    override val name: String
        get() = psi.name.orAnonymous()

    override val nameElement by lz { KotlinConverter.asSimpleReference(psi.nameIdentifier, this) }

    override val fqName: String?
        get() = psi.fqName?.asString()

    override val kind by lz {
        when {
            psi.isAnnotation() -> UastClassKind.ANNOTATION
            (psi as? KtObjectDeclaration)?.isCompanion() ?: false -> {
                if (psi.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.toString())
                    KotlinClassKinds.DEFAULT_COMPANION_OBJECT
                else
                    KotlinClassKinds.NAMED_COMPANION_OBJECT
            }
            psi is KtObjectDeclaration -> UastClassKind.OBJECT
            (psi as? KtClass)?.isInterface() ?: false -> UastClassKind.INTERFACE
            (psi as? KtClass)?.isEnum() ?: false -> UastClassKind.ENUM
            else -> UastClassKind.CLASS
        }
    }

    override val defaultType by lz {
        val type = resolveToDescriptor()?.defaultType ?: return@lz UastErrorType
        KotlinConverter.convert(type, psi.project, this)
    }

    override val companions by lz {
        (psi as? KtClass)?.getCompanionObjects()?.map { KotlinConverter.convert(it, this) } ?: emptyList()
    }

    override val isAnonymous = false

    override val internalName by lz {
        val descriptor = resolveToDescriptor() ?: return@lz null
        val typeMapper = JetTypeMapper(BindingContext.EMPTY, ClassBuilderMode.LIGHT_CLASSES, NoResolveFileClassesProvider, null,
                                       IncompatibleClassTracker.DoNothing, JvmAbi.DEFAULT_MODULE_NAME)
        typeMapper.mapClass(descriptor).internalName
    }

    override fun getSuperClass(context: UastContext): UClass? {
        val superClass = resolveToDescriptor()?.getSuperClassOrAny() ?: return null
        val source = DescriptorToSourceUtilsIde.getAnyDeclaration(psi.project, superClass) ?: return null
        return context.convert(source) as? UClass
    }

    override fun hasModifier(modifier: UastModifier) = psi.hasModifier(modifier)

    override val declarations by lz {
        val primaryConstructor = psi.getPrimaryConstructor()?.let { KotlinConverter.convert(it, this) }
        val anonymousInitializers = psi.getAnonymousInitializers().map { KotlinConverter.convert(it, this) }.filterNotNull()
        val declarations = psi.declarations.map { KotlinConverter.convert(it, this) }.filterNotNull()

        if (primaryConstructor != null)
            listOf(primaryConstructor) + declarations + anonymousInitializers
        else
            declarations + anonymousInitializers
    }

    override val superTypes by lz {
        val superTypes = resolveToDescriptor()?.typeConstructor?.supertypes ?: return@lz emptyList<UType>()
        superTypes.map { KotlinConverter.convert(it, psi.project, this) }
    }

    override val annotations by lz { psi.getUastAnnotations(this) }

    override val visibility by lz { psi.getVisibility() }

    override fun isSubclassOf(name: String): Boolean {
        val descriptor = psi.resolveToDescriptorIfAny() as? ClassDescriptor ?: return false
        return descriptor.defaultType.supertypes().any {
            it.constructor.declarationDescriptor?.fqNameSafe?.asString() == name
        }
    }

    private fun resolveToDescriptor(): ClassDescriptor? {
        val bindingContext = psi.analyze(BodyResolveMode.PARTIAL)
        return bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, psi] as? ClassDescriptor
    }
}