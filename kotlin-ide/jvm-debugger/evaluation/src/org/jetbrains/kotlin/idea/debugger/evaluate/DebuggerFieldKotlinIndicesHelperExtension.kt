/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.extension.KotlinIndicesHelperExtension
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.synthetic.JavaSyntheticPropertiesScope
import org.jetbrains.kotlin.types.KotlinType
import java.lang.IllegalStateException

class DebuggerFieldKotlinIndicesHelperExtension : KotlinIndicesHelperExtension {
    override fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean,
        lookupLocation: LookupLocation
    ) {
        val javaPropertiesScope = JavaSyntheticPropertiesScope(LockBasedStorageManager.NO_LOCKS, LookupTracker.DO_NOTHING)
        val fieldScope = DebuggerFieldSyntheticScope(javaPropertiesScope)

        for (property in fieldScope.getSyntheticExtensionProperties(receiverTypes, lookupLocation)) {
            if (nameFilter(property.name.asString())) {
                consumer += property
            }
        }
    }

    override fun appendExtensionCallables(
        consumer: MutableList<in CallableDescriptor>,
        moduleDescriptor: ModuleDescriptor,
        receiverTypes: Collection<KotlinType>,
        nameFilter: (String) -> Boolean
    ) {
        throw IllegalStateException("Should not be called")
    }
}