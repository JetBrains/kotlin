/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.repl

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.codegen.JvmBackendClassResolver
import org.jetbrains.kotlin.codegen.classId
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.org.objectweb.asm.Type

@OptIn(ObsoleteDescriptorBasedAPI::class)
class K1JvmBackendClassResolverForModuleWithDependencies(
    private val moduleDescriptor: ModuleDescriptor,
    private val symbolTable: Lazy<SymbolTable>,
) : JvmBackendClassResolver {
    override fun resolveToClasses(type: Type): List<IrClass> {
        if (type.sort != Type.OBJECT) return emptyList()

        val platformClass = moduleDescriptor.findClassAcrossModuleDependencies(type.classId) ?: return emptyList()

        return (JavaToKotlinClassMapper.mapPlatformClass(platformClass) + platformClass).map {
            symbolTable.value.lazyWrapper.descriptorExtension.referenceClass(it).owner
        }
    }
}
