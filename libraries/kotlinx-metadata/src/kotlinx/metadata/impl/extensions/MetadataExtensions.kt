/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.impl.extensions

import kotlinx.metadata.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import java.util.*

interface MetadataExtensions {
    fun readFunctionExtensions(v: KmFunctionVisitor, proto: ProtoBuf.Function, strings: NameResolver, types: TypeTable)

    fun readPropertyExtensions(v: KmPropertyVisitor, proto: ProtoBuf.Property, strings: NameResolver, types: TypeTable)

    fun readConstructorExtensions(v: KmConstructorVisitor, proto: ProtoBuf.Constructor, strings: NameResolver, types: TypeTable)

    fun readTypeParameterExtensions(v: KmTypeParameterVisitor, proto: ProtoBuf.TypeParameter, strings: NameResolver)

    fun readTypeExtensions(v: KmTypeVisitor, proto: ProtoBuf.Type, strings: NameResolver)

    companion object {
        val INSTANCE: MetadataExtensions by lazy {
            ServiceLoader.load(MetadataExtensions::class.java).toList().firstOrNull()
                    ?: error(
                        "No MetadataExtensions instances found in the classpath. Please ensure that the META-INF/services/ " +
                                "is not stripped from your application and that the Java virtual machine is not running " +
                                "under a security manager"
                    )
        }
    }
}
