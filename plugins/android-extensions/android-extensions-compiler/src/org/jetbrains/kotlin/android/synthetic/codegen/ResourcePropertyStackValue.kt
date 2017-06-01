/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.synthetic.codegen

import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.res.AndroidSyntheticProperty
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.types.lowerIfFlexible
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class ResourcePropertyStackValue(
        val receiver: StackValue,
        val typeMapper: KotlinTypeMapper,
        val propertyDescriptor: PropertyDescriptor,
        val receiverDescriptor: ClassDescriptor,
        val androidClassType: AndroidClassType,
        val androidPackage: String
) : StackValue(typeMapper.mapType(propertyDescriptor.returnType!!)) {

    override fun putSelector(type: Type, v: InstructionAdapter) {
        val returnTypeString = typeMapper.mapType(propertyDescriptor.type.lowerIfFlexible()).className
        if (AndroidConst.FRAGMENT_FQNAME == returnTypeString || AndroidConst.SUPPORT_FRAGMENT_FQNAME == returnTypeString) {
            return putSelectorForFragment(v)
        }

        val syntheticProperty = propertyDescriptor as AndroidSyntheticProperty

        if (androidClassType.supportsCache && AndroidExpressionCodegenExtension.shouldCacheResource(receiverDescriptor, propertyDescriptor)) {
            val declarationDescriptorType = typeMapper.mapType(receiverDescriptor)
            receiver.put(declarationDescriptorType, v)

            val resourceId = syntheticProperty.resource.id
            val packageName = resourceId.packageName ?: androidPackage
            v.getstatic(packageName.replace(".", "/") + "/R\$id", resourceId.name, "I")

            v.invokevirtual(declarationDescriptorType.internalName, AndroidExpressionCodegenExtension.CACHED_FIND_VIEW_BY_ID_METHOD_NAME, "(I)Landroid/view/View;", false)
        }
        else {
            when (androidClassType) {
                AndroidClassType.ACTIVITY, AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY, AndroidClassType.VIEW, AndroidClassType.DIALOG -> {
                    receiver.put(Type.getType("L${androidClassType.internalClassName};"), v)
                    getResourceId(v)
                    v.invokevirtual(androidClassType.internalClassName, "findViewById", "(I)Landroid/view/View;", false)
                }
                AndroidClassType.FRAGMENT, AndroidClassType.SUPPORT_FRAGMENT -> {
                    receiver.put(Type.getType("L${androidClassType.internalClassName};"), v)
                    v.invokevirtual(androidClassType.internalClassName, "getView", "()Landroid/view/View;", false)
                    getResourceId(v)
                    v.invokevirtual("android/view/View", "findViewById", "(I)Landroid/view/View;", false)
                }
                else -> throw IllegalStateException("Invalid Android class type: $androidClassType") // Should never occur
            }
        }

        v.checkcast(this.type)
    }

    private fun putSelectorForFragment(v: InstructionAdapter) {
        receiver.put(Type.getType("L${androidClassType.internalClassName};"), v)

        when (androidClassType) {
            AndroidClassType.ACTIVITY, AndroidClassType.FRAGMENT -> {
                v.invokevirtual(androidClassType.internalClassName, "getFragmentManager", "()Landroid/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("android/app/FragmentManager", "findFragmentById", "(I)Landroid/app/Fragment;", false)
            }
            AndroidClassType.SUPPORT_FRAGMENT -> {
                v.invokevirtual(androidClassType.internalClassName, "getFragmentManager", "()Landroid/support/v4/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("android/support/v4/app/FragmentManager", "findFragmentById", "(I)Landroid/support/v4/app/Fragment;", false)
            }
            AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY -> {
                v.invokevirtual(androidClassType.internalClassName, "getSupportFragmentManager", "()Landroid/support/v4/app/FragmentManager;", false)
                getResourceId(v)
                v.invokevirtual("android/support/v4/app/FragmentManager", "findFragmentById", "(I)Landroid/support/v4/app/Fragment;", false)
            }
            else -> throw IllegalStateException("Invalid Android class type: $androidClassType") // Should never occur
        }

        v.checkcast(this.type)
    }

    fun getResourceId(v: InstructionAdapter) {
        v.getstatic(androidPackage.replace(".", "/") + "/R\$id", propertyDescriptor.name.asString(), "I")
    }
}