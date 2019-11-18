/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl.extensions

import kotlinx.metadata.*

interface KmExtension<V : KmExtensionVisitor> : KmExtensionVisitor {
    fun accept(visitor: V)
}

interface KmClassExtension : KmClassExtensionVisitor, KmExtension<KmClassExtensionVisitor>

interface KmPackageExtension : KmPackageExtensionVisitor, KmExtension<KmPackageExtensionVisitor>

interface KmModuleFragmentExtension : KmModuleFragmentExtensionVisitor, KmExtension<KmModuleFragmentExtensionVisitor>

interface KmFunctionExtension : KmFunctionExtensionVisitor, KmExtension<KmFunctionExtensionVisitor>

interface KmPropertyExtension : KmPropertyExtensionVisitor, KmExtension<KmPropertyExtensionVisitor>

interface KmConstructorExtension : KmConstructorExtensionVisitor, KmExtension<KmConstructorExtensionVisitor>

interface KmTypeParameterExtension : KmTypeParameterExtensionVisitor, KmExtension<KmTypeParameterExtensionVisitor>

interface KmTypeExtension : KmTypeExtensionVisitor, KmExtension<KmTypeExtensionVisitor>

interface KmTypeAliasExtension : KmTypeAliasExtensionVisitor, KmExtension<KmTypeExtensionVisitor>