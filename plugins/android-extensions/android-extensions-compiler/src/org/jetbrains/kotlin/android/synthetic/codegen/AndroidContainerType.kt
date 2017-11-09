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

import kotlinx.android.extensions.LayoutContainer
import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils

enum class AndroidContainerType(className: String, val doesSupportCache: Boolean = false, val isFragment: Boolean = false) {
    ACTIVITY(AndroidConst.ACTIVITY_FQNAME, doesSupportCache = true),
    FRAGMENT(AndroidConst.FRAGMENT_FQNAME, doesSupportCache = true, isFragment = true),
    DIALOG(AndroidConst.DIALOG_FQNAME, doesSupportCache = false),
    SUPPORT_FRAGMENT_ACTIVITY(AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME, doesSupportCache = true),
    SUPPORT_FRAGMENT(AndroidConst.SUPPORT_FRAGMENT_FQNAME, doesSupportCache = true, isFragment = true),
    VIEW(AndroidConst.VIEW_FQNAME, doesSupportCache = true),
    LAYOUT_CONTAINER(LayoutContainer::class.java.canonicalName, doesSupportCache = true),
    UNKNOWN("");

    val internalClassName: String = className.replace('.', '/')

    companion object {
        private val LAYOUT_CONTAINER_FQNAME = LayoutContainer::class.java.canonicalName

        fun get(descriptor: ClassifierDescriptor): AndroidContainerType {
            fun getContainerTypeInternal(name: String): AndroidContainerType? = when (name) {
                AndroidConst.ACTIVITY_FQNAME -> AndroidContainerType.ACTIVITY
                AndroidConst.FRAGMENT_FQNAME -> AndroidContainerType.FRAGMENT
                AndroidConst.DIALOG_FQNAME -> AndroidContainerType.DIALOG
                AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME -> AndroidContainerType.SUPPORT_FRAGMENT_ACTIVITY
                AndroidConst.SUPPORT_FRAGMENT_FQNAME -> AndroidContainerType.SUPPORT_FRAGMENT
                AndroidConst.VIEW_FQNAME -> AndroidContainerType.VIEW
                LAYOUT_CONTAINER_FQNAME -> AndroidContainerType.LAYOUT_CONTAINER
                else -> null
            }

            getContainerTypeInternal(DescriptorUtils.getFqName(descriptor).asString())?.let { return it }

            for (supertype in descriptor.typeConstructor.supertypes) {
                val declarationDescriptor = supertype.constructor.declarationDescriptor
                if (declarationDescriptor != null) {
                    val containerType = get(declarationDescriptor)
                    if (containerType != AndroidContainerType.UNKNOWN) return containerType
                }
            }

            return AndroidContainerType.UNKNOWN
        }
    }
}