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

package org.jetbrains.kotlin.android.synthetic.res

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.descriptors.SyntheticElementResolveContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.load.java.lazy.types.LazyJavaTypeResolver
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.asSimpleType
import org.jetbrains.kotlin.types.typeUtil.makeNullable

private class XmlSourceElement(override val psi: PsiElement) : PsiSourceElement

internal fun genClearCacheFunction(packageFragmentDescriptor: PackageFragmentDescriptor, receiverType: KotlinType): SimpleFunctionDescriptor {
    val function = object : AndroidSyntheticFunction, SimpleFunctionDescriptorImpl(
            packageFragmentDescriptor,
            null,
            Annotations.EMPTY,
            Name.identifier(AndroidConst.CLEAR_FUNCTION_NAME),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            SourceElement.NO_SOURCE) {}

    val unitType = packageFragmentDescriptor.builtIns.unitType
    function.initialize(receiverType, null, emptyList(), emptyList(), unitType, Modality.FINAL, Visibilities.PUBLIC)
    return function
}

internal fun genPropertyForWidget(
        packageFragmentDescriptor: AndroidSyntheticPackageFragmentDescriptor,
        receiverType: KotlinType,
        resolvedWidget: ResolvedWidget,
        context: SyntheticElementResolveContext
): PropertyDescriptor {
    val sourceEl = resolvedWidget.widget.sourceElement?.let { XmlSourceElement(it) } ?: SourceElement.NO_SOURCE

    val classDescriptor = resolvedWidget.viewClassDescriptor
    val type = classDescriptor?.let {
        val defaultType = classDescriptor.defaultType
        if (defaultType.constructor.parameters.isEmpty())
            defaultType
        else
            KotlinTypeFactory.simpleNotNullType(Annotations.EMPTY, classDescriptor,
                                                defaultType.constructor.parameters.map { StarProjectionImpl(it) })
    } ?: context.viewType

    return genProperty(resolvedWidget.widget.id, receiverType, type, packageFragmentDescriptor, sourceEl, resolvedWidget.errorType)
}

internal fun genPropertyForFragment(
        packageFragmentDescriptor: AndroidSyntheticPackageFragmentDescriptor,
        receiverType: KotlinType,
        type: KotlinType,
        fragment: AndroidResource.Fragment
): PropertyDescriptor {
    val sourceElement = fragment.sourceElement?.let { XmlSourceElement(it) } ?: SourceElement.NO_SOURCE
    return genProperty(fragment.id, receiverType, type, packageFragmentDescriptor, sourceElement, null)
}

private fun genProperty(
        id: ResourceIdentifier,
        receiverType: KotlinType,
        type: KotlinType,
        containingDeclaration: AndroidSyntheticPackageFragmentDescriptor,
        sourceElement: SourceElement,
        errorType: String?
): PropertyDescriptor {
    val cacheView = type.constructor.declarationDescriptor?.fqNameUnsafe?.asString() != AndroidConst.VIEWSTUB_FQNAME

    val property = object : AndroidSyntheticProperty, PropertyDescriptorImpl(
            containingDeclaration,
            null,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PUBLIC,
            false,
            Name.identifier(id.name),
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            sourceElement,
            false,
            false) {
        override val errorType = errorType
        override val cacheView = cacheView
        override val resourceId = id
    }

    val flexibleType = KotlinTypeFactory.flexibleType(type.asSimpleType(), type.makeNullable().asSimpleType())
    property.setType(
            flexibleType,
            emptyList<TypeParameterDescriptor>(),
            null,
            receiverType)

    val getter = PropertyGetterDescriptorImpl(
            property,
            Annotations.EMPTY,
            Modality.FINAL,
            Visibilities.PUBLIC,
            false,
            false,
            CallableMemberDescriptor.Kind.SYNTHESIZED,
            null,
            SourceElement.NO_SOURCE
    )

    getter.initialize(null)

    property.initialize(getter, null)

    return property
}

interface AndroidSyntheticFunction

interface AndroidSyntheticProperty {
    val errorType: String?
    val cacheView: Boolean
    val resourceId: ResourceIdentifier

    val isErrorType: Boolean
        get() = errorType != null
}