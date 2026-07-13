/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.codegen.JvmBackendClassResolver
import org.jetbrains.kotlin.codegen.classId
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.org.objectweb.asm.Type

class K1JvmBackendClassResolverForModuleWithDependencies(
    private val moduleDescriptor: ModuleDescriptor
) : JvmBackendClassResolver {

    override fun resolveToClassDescriptors(type: Type): List<ClassDescriptor> {
        if (type.sort != Type.OBJECT) return emptyList()

        val platformClass = moduleDescriptor.findClassAcrossModuleDependencies(type.classId) ?: return emptyList()

        return JavaToKotlinClassMapper.mapPlatformClass(platformClass) + platformClass
    }
}
