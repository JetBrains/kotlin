/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DEPRECATION_ERROR") // deprecated visitors API

package kotlin.metadata.internal.extensions

import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragmentExtensionVisitor

public interface KmExtension<V : KmExtensionVisitor> : KmExtensionVisitor {
    public fun accept(visitor: V)
}

public interface KmClassExtension : KmClassExtensionVisitor, KmExtension<KmClassExtensionVisitor>

public interface KmPackageExtension : KmPackageExtensionVisitor, KmExtension<KmPackageExtensionVisitor>

public interface KmModuleFragmentExtension : KmModuleFragmentExtensionVisitor, KmExtension<KmModuleFragmentExtensionVisitor>

public interface KmFunctionExtension : KmFunctionExtensionVisitor, KmExtension<KmFunctionExtensionVisitor>

public interface KmPropertyExtension : KmPropertyExtensionVisitor, KmExtension<KmPropertyExtensionVisitor>

public interface KmConstructorExtension : KmConstructorExtensionVisitor, KmExtension<KmConstructorExtensionVisitor>

public interface KmTypeParameterExtension : KmTypeParameterExtensionVisitor, KmExtension<KmTypeParameterExtensionVisitor>

public interface KmTypeExtension : KmTypeExtensionVisitor, KmExtension<KmTypeExtensionVisitor>

public interface KmTypeAliasExtension : KmTypeAliasExtensionVisitor, KmExtension<KmTypeAliasExtensionVisitor>

public interface KmValueParameterExtension : KmValueParameterExtensionVisitor, KmExtension<KmValueParameterExtensionVisitor>
