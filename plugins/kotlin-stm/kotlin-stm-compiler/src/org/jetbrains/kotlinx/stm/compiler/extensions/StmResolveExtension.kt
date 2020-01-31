/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.stm.compiler.extensions

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.Name.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlinx.stm.compiler.SHARABLE_NAME_SUFFIX
import org.jetbrains.kotlinx.stm.compiler.SHARED_MUTABLE_ANNOTATION
import java.util.ArrayList


open class StmResolveExtension : SyntheticResolveExtension {

    companion object {
        internal fun shareFqName(name: FqName): Name =
            identifier("$name${SHARABLE_NAME_SUFFIX}")

        internal fun sharedName(name: Name): Name =
            identifier("$name${SHARABLE_NAME_SUFFIX}")

        internal fun immutableName(name: Name): Name =
            identifier(name.asString().removeSuffix(SHARABLE_NAME_SUFFIX))

        internal fun nestedDelegateName(thisDescriptor: ClassDescriptor): Name =
            shareFqName(thisDescriptor.fqNameSafe)
    }

    override fun generateSyntheticProperties(
        thisDescriptor: ClassDescriptor,
        name: Name,
        bindingContext: BindingContext,
        fromSupertypes: ArrayList<PropertyDescriptor>,
        result: MutableSet<PropertyDescriptor>
    ) {
        if (thisDescriptor.annotations.hasAnnotation(SHARED_MUTABLE_ANNOTATION)) {

        }
    }

//    override fun getSyntheticNestedClassNames(thisDescriptor: ClassDescriptor): List<Name> =
//        if (thisDescriptor.annotations.hasAnnotation(sharedMutableAnnotation))
//            listOf(nestedDelegateName(thisDescriptor))
//        else
//            listOf()
//
//    override fun generateSyntheticClasses(
//        thisDescriptor: ClassDescriptor,
//        name: Name,
//        ctx: LazyClassContext,
//        declarationProvider: ClassMemberDeclarationProvider,
//        result: MutableSet<ClassDescriptor>
//    ) {
//        if (thisDescriptor.annotations.hasAnnotation(sharedMutableAnnotation) && name == nestedDelegateName(thisDescriptor)) {
//            val thisDeclaration = declarationProvider.correspondingClassOrObject!!
//            val scope = ctx.declarationScopeProvider.getResolutionScopeForDeclaration(declarationProvider.ownerInfo!!.scopeAnchor)
//            val sharedDelegateDescriptor = SyntheticClassOrObjectDescriptor(
//                ctx,
//                parentClassOrObject = thisDeclaration,
//                containingDeclaration = thisDescriptor,
//                name = name,
//                source = thisDescriptor.source,
//                outerScope = scope,
//                modality = Modality.FINAL,
//                visibility = Visibilities.PRIVATE,
//                annotations = Annotations.EMPTY,
//                constructorVisibility = Visibilities.DEFAULT_VISIBILITY,
//                kind = ClassKind.CLASS,
//                isCompanionObject = false
//            )
//            sharedDelegateDescriptor.initialize(thisDescriptor.declaredTypeParameters)
//            result += sharedDelegateDescriptor
//        }
//    }

//    override fun getSyntheticFunctionNames(thisDescriptor: ClassDescriptor): List<Name> {
//        val parent = thisDescriptor.getParentClass() ?: return emptyList()
//        if (thisDescriptor.name.isSharableViaStmDeclaration())
//            return parent.unsubstitutedMemberScope.getFunctionNames().map(::sharedName)
//        return emptyList()
//    }
//
//    override fun generateSyntheticMethods(
//        thisDescriptor: ClassDescriptor,
//        name: Name,
//        bindingContext: BindingContext,
//        fromSupertypes: List<SimpleFunctionDescriptor>,
//        result: MutableCollection<SimpleFunctionDescriptor>
//    ) {
//        if (!name.isSharableViaStmDeclaration() && result.none { it.name == name }) return
//        val parent = thisDescriptor.getParentClass() ?: return
//
//        parent.findMethods(immutableName(name)).forEach { parentMethod ->
//            val functionDescriptor = SimpleFunctionDescriptorImpl.create(
//                thisDescriptor,
//                Annotations.EMPTY,
//                name,
//                CallableMemberDescriptor.Kind.SYNTHESIZED,
//                thisDescriptor.source
//            )
//
//            functionDescriptor.initialize(
//                null,
//                thisDescriptor.thisAsReceiverParameter,
//                parentMethod.typeParameters,
//                parentMethod.valueParameters,
//                DefaultBuiltIns.Instance.intType,
//                Modality.OPEN,
//                Visibilities.PUBLIC
//            )
//
//            result += functionDescriptor
//        }
//    }
}