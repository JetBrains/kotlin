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

enum class AndroidClassType(className: String, val supportsCache: Boolean = false, val fragment: Boolean = false) {
    ACTIVITY(AndroidConst.ACTIVITY_FQNAME, supportsCache = true),
    FRAGMENT(AndroidConst.FRAGMENT_FQNAME, supportsCache = true, fragment = true),
    DIALOG(AndroidConst.DIALOG_FQNAME, supportsCache = false),
    SUPPORT_FRAGMENT_ACTIVITY(AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME, supportsCache = true),
    SUPPORT_FRAGMENT(AndroidConst.SUPPORT_FRAGMENT_FQNAME, supportsCache = true, fragment = true),
    VIEW(AndroidConst.VIEW_FQNAME, supportsCache = true),
    UNKNOWN("");

    val internalClassName: String = className.replace('.', '/')

    companion object {
        fun getClassType(descriptor: ClassifierDescriptor): AndroidClassType {
            fun getClassTypeInternal(name: String): AndroidClassType? = when (name) {
                AndroidConst.ACTIVITY_FQNAME -> AndroidClassType.ACTIVITY
                AndroidConst.FRAGMENT_FQNAME -> AndroidClassType.FRAGMENT
                AndroidConst.DIALOG_FQNAME -> AndroidClassType.DIALOG
                AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME -> AndroidClassType.SUPPORT_FRAGMENT_ACTIVITY
                AndroidConst.SUPPORT_FRAGMENT_FQNAME -> AndroidClassType.SUPPORT_FRAGMENT
                AndroidConst.VIEW_FQNAME -> AndroidClassType.VIEW
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
                    val androidClassType = getClassType(declarationDescriptor)
                    if (androidClassType != AndroidClassType.UNKNOWN) return androidClassType
                }
            }

            return AndroidClassType.UNKNOWN
        }
    }
}