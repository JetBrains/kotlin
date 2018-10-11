/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.android.synthetic.codegen

import kotlinx.android.extensions.CacheImplementation
import kotlinx.android.extensions.LayoutContainer
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.codegen.AbstractAndroidExtensionsExpressionCodegenExtension.Companion.CACHED_FIND_VIEW_BY_ID_METHOD_NAME
import org.jetbrains.kotlin.android.synthetic.codegen.AbstractAndroidExtensionsExpressionCodegenExtension.Companion.shouldCacheResource
import org.jetbrains.kotlin.android.synthetic.descriptors.ContainerOptionsProxy
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class ResourcePropertyStackValue(
        val receiver: StackValue,
        private val typeMapper: KotlinTypeMapper,
        val resource: PropertyDescriptor,
        val container: ClassDescriptor,
        private val containerOptions: ContainerOptionsProxy,
        private val androidPackage: String,
        private val globalCacheImpl: CacheImplementation
) : StackValue(typeMapper.mapType(resource.returnType!!)) {
    private val containerType get() = containerOptions.containerType

    init {
        assert(containerOptions.containerType != AndroidContainerType.UNKNOWN)
    }

    override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
        val returnTypeString = typeMapper.mapType(resource.type.lowerIfFlexible()).className
        if (AndroidConst.FRAGMENT_FQNAME == returnTypeString || AndroidConst.SUPPORT_FRAGMENT_FQNAME == returnTypeString || AndroidConst.ANDROIDX_SUPPORT_FRAGMENT_FQNAME == returnTypeString) {
            return putSelectorForFragment(v)
        }

        val syntheticProperty = resource as AndroidSyntheticProperty

        if ((containerOptions.cache ?: globalCacheImpl).hasCache && shouldCacheResource(resource)) {
            val declarationDescriptorKotlinType = container.defaultType
            val declarationDescriptorType = typeMapper.mapType(declarationDescriptorKotlinType)
            receiver.put(declarationDescriptorType, declarationDescriptorKotlinType, v)

            val resourceId = syntheticProperty.resource.id
            val packageName = resourceId.packageName ?: androidPackage
            v.getstatic(packageName.replace(".", "/") + "/R\$id", resourceId.name, "I")

            v.invokevirtual(declarationDescriptorType.internalName, CACHED_FIND_VIEW_BY_ID_METHOD_NAME, "(I)Landroid/view/View;", false)
        }
        else {
            when (containerType) {
                AndroidContainerType.ACTIVITY, AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT_ACTIVITY, AndroidContainerType.SUPPORT_FRAGMENT_ACTIVITY, AndroidContainerType.VIEW, AndroidContainerType.DIALOG -> {
                    receiver.put(Type.getType("L${containerType.internalClassName};"), v)
                    getResourceId(v)
                    v.invokevirtual(containerType.internalClassName, "findViewById", "(I)Landroid/view/View;", false)
                }
                AndroidContainerType.FRAGMENT, AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT, AndroidContainerType.SUPPORT_FRAGMENT -> {
                    receiver.put(Type.getType("L${containerType.internalClassName};"), v)
                    v.invokevirtual(containerType.internalClassName, "getView", "()Landroid/view/View;", false)
                    getResourceId(v)
                    v.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
                }
                AndroidContainerType.LAYOUT_CONTAINER -> {
                    receiver.put(Type.getType("L${containerType.internalClassName};"), v)
                    v.invokeinterface(Type.getType(LayoutContainer::class.java).internalName, "getContainerView", "()Landroid/view/View;")
                    getResourceId(v)
                    v.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
                }
                else -> throw IllegalStateException("Invalid Android class type: $containerType") // Should never occur
            }
        }

        v.checkcast(this.type)
    }

    private fun putSelectorForFragment(v: InstructionAdapter) {
        receiver.put(Type.getType("L${containerType.internalClassName};"), v)

        when (containerType) {
            AndroidContainerType.ACTIVITY, AndroidContainerType.FRAGMENT -> {
                v.invokevirtual(containerType.internalClassName, "getFragmentManager", "()Landroid/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("android/app/FragmentManager", "findFragmentById", "(I)Landroid/app/Fragment;", false)
            }
            AndroidContainerType.SUPPORT_FRAGMENT -> {
                v.invokevirtual(containerType.internalClassName, "getFragmentManager", "()Landroid/support/v4/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("android/support/v4/app/FragmentManager", "findFragmentById", "(I)Landroid/support/v4/app/Fragment;", false)
            }
            AndroidContainerType.SUPPORT_FRAGMENT_ACTIVITY -> {
                v.invokevirtual(containerType.internalClassName, "getSupportFragmentManager", "()Landroid/support/v4/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("android/support/v4/app/FragmentManager", "findFragmentById", "(I)Landroid/support/v4/app/Fragment;", false)
            }
            AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT -> {
                v.invokevirtual(containerType.internalClassName, "getFragmentManager", "()Landroidx/fragment/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("androidx/fragment/app/FragmentManager", "findFragmentById", "(I)Landroidx/fragment/app/Fragment;", false)
            }
            AndroidContainerType.ANDROIDX_SUPPORT_FRAGMENT_ACTIVITY -> {
                v.invokevirtual(containerType.internalClassName, "getSupportFragmentManager", "()Landroidx/fragment/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("androidx/fragment/app/FragmentManager", "findFragmentById", "(I)Landroidx/fragment/app/Fragment;", false)
            }
            else -> throw IllegalStateException("Invalid Android class type: $containerType") // Should never occur
        }

        v.checkcast(this.type)
    }

    private fun getResourceId(v: InstructionAdapter) {
        v.getstatic(androidPackage.replace(".", "/") + "/R\$id", resource.name.asString(), "I")
    }
}