/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.internal.common

import kotlinx.metadata.KmModuleFragment
import kotlinx.metadata.KmModuleFragmentVisitor
import kotlinx.metadata.impl.accept
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.readBuiltinsPackageFragment
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import java.io.ByteArrayInputStream

class KotlinCommonMetadata private constructor(private val proto: ProtoBuf.PackageFragment) {
    fun toKmModuleFragment(): KmModuleFragment =
        KmModuleFragment().apply(this::accept)

    class Writer : KmModuleFragmentVisitor() {
        // TODO
    }

    fun accept(v: KmModuleFragmentVisitor) {
        val strings = NameResolverImpl(proto.strings, proto.qualifiedNames)
        if (proto.hasPackage()) {
            v.visitPackage()?.let { proto.`package`.accept(it, strings) }
        }
        for (klass in proto.class_List) {
            v.visitClass()?.let { klass.accept(it, strings) }
        }
        v.visitEnd()
    }

    companion object {
        @JvmStatic
        fun read(bytes: ByteArray): KotlinCommonMetadata? {
            val (proto, _) = ByteArrayInputStream(bytes).readBuiltinsPackageFragment()
            if (proto == null) return null

            return KotlinCommonMetadata(proto)
        }
    }
}
