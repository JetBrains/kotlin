/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl.extensions

import kotlinx.metadata.*
import kotlinx.metadata.impl.ReadContext
import kotlinx.metadata.impl.WriteContext
import org.jetbrains.kotlin.metadata.ProtoBuf
import java.util.*

interface MetadataExtensions {
    fun readClassExtensions(v: KmClassVisitor, proto: ProtoBuf.Class, c: ReadContext)

    fun readPackageExtensions(v: KmPackageVisitor, proto: ProtoBuf.Package, c: ReadContext)

    fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, c: ReadContext)

    fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, c: ReadContext)

    fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, c: ReadContext)

    fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, c: ReadContext)

    fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, c: ReadContext)

    fun writeClassExtensions(type: KmExtensionType, proto: ProtoBuf.Class.Builder, c: WriteContext): KmClassExtensionVisitor?

    fun writePackageExtensions(type: KmExtensionType, proto: ProtoBuf.Package.Builder, c: WriteContext): KmPackageExtensionVisitor?

    fun writeFunctionExtensions(type: KmExtensionType, proto: ProtoBuf.Function.Builder, c: WriteContext): KmFunctionExtensionVisitor?

    fun writePropertyExtensions(type: KmExtensionType, proto: ProtoBuf.Property.Builder, c: WriteContext): KmPropertyExtensionVisitor?

    fun writeConstructorExtensions(
        type: KmExtensionType, proto: ProtoBuf.Constructor.Builder, c: WriteContext
    ): KmConstructorExtensionVisitor?

    fun writeTypeParameterExtensions(
        type: KmExtensionType, proto: ProtoBuf.TypeParameter.Builder, c: WriteContext
    ): KmTypeParameterExtensionVisitor?

    fun writeTypeExtensions(type: KmExtensionType, proto: ProtoBuf.Type.Builder, c: WriteContext): KmTypeExtensionVisitor?

    companion object {
        val INSTANCES: List<MetadataExtensions> by lazy {
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
