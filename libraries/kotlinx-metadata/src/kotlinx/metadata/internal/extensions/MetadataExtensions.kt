/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.internal.extensions

import kotlinx.metadata.*
import kotlinx.metadata.internal.*
import kotlinx.metadata.internal.common.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import java.util.*

@Suppress("DEPRECATION")
public interface MetadataExtensions {
    public fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext)

    public fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext)

    public fun readModuleFragmentExtensions(v: KmModuleFragmentVisitor, proto: ProtoBuf.PackageFragment, c: ReadContext)

    public fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext)

    public fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext)

    public fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext)

    public fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext)

    public fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext)

    public fun readTypeAliasExtensions(v: KmTypeAliasVisitor, proto: ProtoBuf.TypeAlias, c: ReadContext)

    public fun readValueParameterExtensions(v: KmValueParameterVisitor, proto: ProtoBuf.ValueParameter, c: ReadContext)

    public fun writeClassExtensions(type: KmExtensionType, proto: ProtoBuf.Class.Builder, c: WriteContext): KmClassExtensionVisitor?

    public fun writePackageExtensions(type: KmExtensionType, proto: ProtoBuf.Package.Builder, c: WriteContext): KmPackageExtensionVisitor?

    public fun writeModuleFragmentExtensions(
        type: KmExtensionType, proto: ProtoBuf.PackageFragment.Builder, c: WriteContext
    ): KmModuleFragmentExtensionVisitor?

    public fun writeFunctionExtensions(type: KmExtensionType, proto: ProtoBuf.Function.Builder, c: WriteContext): KmFunctionExtensionVisitor?

    public fun writePropertyExtensions(type: KmExtensionType, proto: ProtoBuf.Property.Builder, c: WriteContext): KmPropertyExtensionVisitor?

    public fun writeConstructorExtensions(
        type: KmExtensionType, proto: ProtoBuf.Constructor.Builder, c: WriteContext
    ): KmConstructorExtensionVisitor?

    public fun writeTypeParameterExtensions(
        type: KmExtensionType, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext
    ): KmTypeParameterExtensionVisitor?

    public fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor?

    public fun writeTypeAliasExtensions(type: KmExtensionType, proto: ProtoBuf.TypeAlias.Builder, c: WriteContext): KmTypeAliasExtensionVisitor?

    public fun writeValueParameterExtensions(
        type: KmExtensionType, proto: ProtoBuf.ValueParameter.Builder, c: WriteContext
    ): KmValueParameterExtensionVisitor?

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
