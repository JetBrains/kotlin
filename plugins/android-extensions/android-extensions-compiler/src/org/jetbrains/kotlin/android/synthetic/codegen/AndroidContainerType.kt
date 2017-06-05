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
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor

enum class AndroidContainerType(className: String, val doesSupportCache: Boolean = false, val isFragment: Boolean = false) {
    ACTIVITY(AndroidConst.ACTIVITY_FQNAME, doesSupportCache = true),
    FRAGMENT(AndroidConst.FRAGMENT_FQNAME, doesSupportCache = true, isFragment = true),
    DIALOG(AndroidConst.DIALOG_FQNAME, doesSupportCache = false),
    SUPPORT_FRAGMENT_ACTIVITY(AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME, doesSupportCache = true),
    SUPPORT_FRAGMENT(AndroidConst.SUPPORT_FRAGMENT_FQNAME, doesSupportCache = true, isFragment = true),
    VIEW(AndroidConst.VIEW_FQNAME, doesSupportCache = true),
    UNKNOWN("");

    val internalClassName: String = className.replace('.', '/')

    companion object {
        fun get(descriptor: ClassifierDescriptor): AndroidContainerType {
            fun getClassTypeInternal(name: String): AndroidContainerType? = when (name) {
                AndroidConst.ACTIVITY_FQNAME -> AndroidContainerType.ACTIVITY
                AndroidConst.FRAGMENT_FQNAME -> AndroidContainerType.FRAGMENT
                AndroidConst.DIALOG_FQNAME -> AndroidContainerType.DIALOG
                AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME -> AndroidContainerType.SUPPORT_FRAGMENT_ACTIVITY
                AndroidConst.SUPPORT_FRAGMENT_FQNAME -> AndroidContainerType.SUPPORT_FRAGMENT
                AndroidConst.VIEW_FQNAME -> AndroidContainerType.VIEW
                else -> null
            }

            if (descriptor is LazyJavaClassDescriptor) {
                val androidClassType = getClassTypeInternal(DescriptorUtils.getFqName(descriptor).asString())
                if (androidClassType != null) return androidClassType
            }
            else if (descriptor is LazyClassDescriptor) { // For tests (FakeActivity)
                val androidClassType = getClassTypeInternal(DescriptorUtils.getFqName(descriptor).toString())
                if (androidClassType != null) return androidClassType
            }

            for (supertype in descriptor.typeConstructor.supertypes) {
                val declarationDescriptor = supertype.constructor.declarationDescriptor
                if (declarationDescriptor != null) {
                    val androidClassType = get(declarationDescriptor)
                    if (androidClassType != AndroidContainerType.UNKNOWN) return androidClassType
                }
            }

            return AndroidContainerType.UNKNOWN
        }
    }
}