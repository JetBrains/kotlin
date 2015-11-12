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

package org.jetbrains.kotlin.android.synthetic.idea

import org.jetbrains.kotlin.android.synthetic.AndroidConst
import org.jetbrains.kotlin.android.synthetic.descriptors.AndroidSyntheticPackageFragmentDescriptor
import org.jetbrains.kotlin.android.synthetic.descriptors.PredefinedPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class AndroidIndicesHelperExtension : KotlinIndicesHelperExtension {

    override fun appendExtensionCallables(
            consumer: MutableList<in CallableDescriptor>,
            moduleDescriptor: ModuleDescriptor,
            receiverTypes: Collection<KotlinType>,
            nameFilter: (String) -> Boolean
    ) {
        for (packageFragment in moduleDescriptor.getPackage(FqName(AndroidConst.SYNTHETIC_PACKAGE)).fragments) {
            if (packageFragment !is PredefinedPackageFragmentDescriptor) continue

            fun handleScope(scope: MemberScope) {
                val descriptors = scope.getContributedDescriptors(DescriptorKindFilter.CALLABLES) { nameFilter(it.asString()) }
                for (descriptor in descriptors) {
                    val receiverType = (descriptor as CallableDescriptor).extensionReceiverParameter?.type ?: continue
                    if (receiverTypes.any { it.isSubtypeOf(receiverType) }) {
                        consumer += descriptor
                    }
                }
            }

            handleScope(packageFragment.getMemberScope())
            for (fragment in packageFragment.subpackages) {
                if (fragment is AndroidSyntheticPackageFragmentDescriptor && fragment.packageData.isDeprecated) continue
                handleScope(fragment.getMemberScope())
            }
        }
    }
}