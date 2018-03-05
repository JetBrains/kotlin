/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.test

import kotlinx.metadata.*
import kotlinx.metadata.jvm.JvmFunctionExtensionVisitor
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataSmokeTest {
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    private fun Class<*>.readMetadata(): KotlinClassHeader {
        return getAnnotation(Metadata::class.java).run {
            KotlinClassHeader(k, mv, bv, d1, d2, xs, pn, xi)
        }
    }

    @Test
    fun listInlineFunctions() {
        @Suppress("unused")
        class L {
            val x: Int inline get() = 42
            inline fun foo(f: () -> String) = f()
            fun bar() {}
        }

        val inlineFunctions = mutableListOf<String>()

        val klass = KotlinClassMetadata.read(L::class.java.readMetadata()) as KotlinClassMetadata.Class
        klass.accept(object : KmClassVisitor() {
            override fun visitFunction(flags: Int, name: String): KmFunctionVisitor? {
                return object : KmFunctionVisitor() {
                    override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? {
                        if (type != JvmFunctionExtensionVisitor.TYPE) return null

                        return object : JvmFunctionExtensionVisitor() {
                            override fun visit(desc: String?) {
                                if (Flags.Function.IS_INLINE(flags) && desc != null) {
                                    inlineFunctions += desc
                                }
                            }
                        }
                    }
                }
            }
        })

        assertEquals(
            listOf("foo(Lkotlin/jvm/functions/Function0;)Ljava/lang/String;"),
            inlineFunctions
        )
    }
}
