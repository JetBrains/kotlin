/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.metadata.internal.common

import kotlin.metadata.*
import kotlin.metadata.internal.toKmPackage
import kotlin.metadata.internal.toKmClass
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.readBuiltinsPackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import java.io.ByteArrayInputStream
import kotlin.metadata.internal.extensions.*

/**
 * Reads metadata that is not from annotation nor from `.kotlin_module` file.
 * Usually such metadata comes from serialized klibs. However, special `kotlin_builtins` file from standard library
 * distribution can also be read with this reader.
 */
public class KotlinCommonMetadata private constructor(proto: ProtoBuf.PackageFragment) {

    public val kmModuleFragment: KmModuleFragment = readImpl(proto)

    // private because there are no use-cases and it is not finished
    private class Writer {
        // TODO
    }

    private fun readImpl(proto: ProtoBuf.PackageFragment): KmModuleFragment {
        val v = KmModuleFragment()
        val strings = NameResolverImpl(proto.strings, proto.qualifiedNames)
        if (proto.hasPackage()) {
           v.pkg = proto.`package`.toKmPackage(strings)
        }
        proto.class_List.mapTo(v.classes) { it.toKmClass(strings) }
        return v
    }

    public companion object {
        @JvmStatic
        public fun read(bytes: ByteArray): KotlinCommonMetadata? {
            val (proto, _) = ByteArrayInputStream(bytes).readBuiltinsPackageFragment()
            if (proto == null) return null

            return KotlinCommonMetadata(proto)
        }
    }
}

/**
 * Represents a Kotlin module fragment.
 *
 * Do not confuse with `KmModule`: while KmModule represents JVM-specific `.kotlin_module` file, KmModuleFragment is not platform-specific.
 * It usually represents metadata serialized to klib or part of klib,
 * but also may represent a special `.kotlin_builtins` file that can be encountered only in standard library.
 *
 * Can be read with [KotlinCommonMetadata.read].
 */
public class KmModuleFragment {

    /**
     * Top-level functions, type aliases and properties in the module fragment.
     */
    public var pkg: KmPackage? = null

    /**
     * Classes in the module fragment.
     */
    public val classes: MutableList<KmClass> = ArrayList()

    internal val extensions: List<KmModuleFragmentExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createModuleFragmentExtensions)
}
