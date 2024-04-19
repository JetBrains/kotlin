/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.lower.hiddenfromobjc

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

/**
 * Represents a set of declarations that should have
 * kotlin.native.HiddenFromObjC annotation added in their IR and descriptors.
 *
 * It's used by [AddHiddenFromObjCSerializationPlugin] to determine
 * if there's a need to modify the declaration descriptor before it's serialized.
 * This set is populated by [AddHiddenFromObjCLowering].
 *
 * More context:
 * The reason why we need this set is due to k/native ObjCExportMapper.kt is
 * using descriptors to look at the declaration annotations.
 * When ObjCExportMapper.kt migrates to FIR, we will need to simply remove this interface
 * and [AddHiddenFromObjCSerializationPlugin].
 * Adding the annotation in IR - [AddHiddenFromObjCLowering] will likely be enough.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class HideFromObjCDeclarationsSet {

    private val set = mutableSetOf<FqName>()

    fun add(function: IrFunction) {
        set.add(function.descriptor.fqNameSafe)
    }

    fun add(property: IrProperty) {
        set.add(property.descriptor.fqNameSafe)
    }

    fun add(cls: IrClass) {
        set.add(cls.descriptor.fqNameSafe)
    }

    operator fun contains(item: DeclarationDescriptor): Boolean {
        return set.contains(item.fqNameSafe)
    }
}
