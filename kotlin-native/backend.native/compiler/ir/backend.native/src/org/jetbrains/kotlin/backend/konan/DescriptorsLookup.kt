/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

internal class DescriptorsLookup(val builtIns: KonanBuiltIns) {
    private val packageScope by lazy { builtIns.builtInsModule.getPackage(KonanFqNames.internalPackageName).memberScope }

    val nativePtr by lazy { packageScope.getContributedClassifier(NATIVE_PTR_NAME) as ClassDescriptor }
    val getNativeNullPtr by lazy { packageScope.getContributedFunctions("getNativeNullPtr").single() }
    val immutableBlobOf by lazy {
        builtIns.builtInsModule.getPackage(KonanFqNames.packageName).memberScope.getContributedFunctions("immutableBlobOf").single()
    }

    val interopBuiltIns by lazy {
        InteropBuiltIns(this.builtIns)
    }

    private fun MemberScope.getContributedClassifier(name: String) =
            this.getContributedClassifier(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)

    private fun MemberScope.getContributedFunctions(name: String) =
            this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BUILTINS)
}