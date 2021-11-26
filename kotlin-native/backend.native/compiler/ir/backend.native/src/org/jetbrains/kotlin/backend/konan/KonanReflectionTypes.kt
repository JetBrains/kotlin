/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan


import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_REFLECT_FQ_NAME
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class KonanReflectionTypes(module: ModuleDescriptor) {

    private val kotlinReflectScope: MemberScope by lazy(LazyThreadSafetyMode.PUBLICATION) {
        module.getPackage(KOTLIN_REFLECT_FQ_NAME).memberScope
    }


    private fun find(memberScope: MemberScope, className: String): ClassDescriptor {
        val name = Name.identifier(className)
        return memberScope.getContributedClassifier(name, NoLookupLocation.FROM_REFLECTION) as ClassDescriptor
    }

    fun getKFunction(n: Int): ClassDescriptor = find(kotlinReflectScope, "KFunction$n")

    fun getKSuspendFunction(n: Int): ClassDescriptor = find(kotlinReflectScope, "KSuspendFunction$n")
}
