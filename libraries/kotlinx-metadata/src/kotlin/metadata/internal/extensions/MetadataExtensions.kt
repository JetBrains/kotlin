/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.internal.extensions

import kotlin.metadata.*
import kotlin.metadata.internal.*
import kotlin.metadata.internal.common.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import java.util.*

public interface MetadataExtensions {
    public fun readClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class, c: ReadContext)

    public fun readPackageExtensions(kmPackage: KmPackage, proto: ProtoBuf.Package, c: ReadContext)

    public fun readModuleFragmentExtensions(kmModuleFragment: KmModuleFragment, proto: ProtoBuf.PackageFragment, c: ReadContext)

    public fun readFunctionExtensions(kmFunction: KmFunction, proto: ProtoBuf.Function, c: ReadContext)

    public fun readPropertyExtensions(kmProperty: KmProperty, proto: ProtoBuf.Property, c: ReadContext)

    public fun readConstructorExtensions(kmConstructor: KmConstructor, proto: ProtoBuf.Constructor, c: ReadContext)

    public fun readTypeParameterExtensions(kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter, c: ReadContext)

    public fun readTypeExtensions(kmType: KmType, proto: ProtoBuf.Type, c: ReadContext)

    public fun readTypeAliasExtensions(kmTypeAlias: KmTypeAlias, proto: ProtoBuf.TypeAlias, c: ReadContext)

    public fun readValueParameterExtensions(kmValueParameter: KmValueParameter, proto: ProtoBuf.ValueParameter, c: ReadContext)

    public fun writeClassExtensions(kmClass: KmClass, proto: ProtoBuf.Class.Builder, c: WriteContext)

    public fun writePackageExtensions(kmPackage: KmPackage, proto: ProtoBuf.Package.Builder, c: WriteContext)

    public fun writeModuleFragmentExtensions(
        kmModuleFragment: KmModuleFragment, proto: ProtoBuf.PackageFragment.Builder, c: WriteContext
    )

    public fun writeFunctionExtensions(kmFunction: KmFunction, proto: ProtoBuf.Function.Builder, c: WriteContext)

    public fun writePropertyExtensions(kmProperty: KmProperty, proto: ProtoBuf.Property.Builder, c: WriteContext)

    public fun writeConstructorExtensions(
        kmConstructor: KmConstructor, proto: ProtoBuf.Constructor.Builder, c: WriteContext
    )

    public fun writeTypeParameterExtensions(
        kmTypeParameter: KmTypeParameter, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext
    )

    public fun writeTypeExtensions(type: KmType, proto: ProtoBuf.Type.Builder, c: WriteContext)

    public fun writeTypeAliasExtensions(typeAlias: KmTypeAlias, proto: ProtoBuf.TypeAlias.Builder, c: WriteContext)

    public fun writeValueParameterExtensions(
        valueParameter: KmValueParameter, proto: ProtoBuf.ValueParameter.Builder, c: WriteContext
    )

    public fun createClassExtension(): KmClassExtension

    public fun createPackageExtension(): KmPackageExtension

    public fun createModuleFragmentExtensions(): KmModuleFragmentExtension

    public fun createFunctionExtension(): KmFunctionExtension

    public fun createPropertyExtension(): KmPropertyExtension

    public fun createConstructorExtension(): KmConstructorExtension

    public fun createTypeParameterExtension(): KmTypeParameterExtension

    public fun createTypeExtension(): KmTypeExtension

    public fun createTypeAliasExtension(): KmTypeAliasExtension?

    public fun createValueParameterExtension(): KmValueParameterExtension?

    // 'internal' is not applicable inside an interface: KT-59796
    public companion object {
        internal val INSTANCES: List<MetadataExtensions> by lazy {
            ServiceLoader.load(MetadataExtensions::class.java, MetadataExtensions::class.java.classLoader).toList().also {
                if (it.isEmpty()) error(
                    "No MetadataExtensions instances found in the classpath. Please ensure that the META-INF/services/ " +
                            "is not stripped from your application and that the Java virtual machine is not running " +
                            "under a security manager"
                )
            }
        }
    }
}
