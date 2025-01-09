/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.abi.tools.naming.jvmInternalToCanonical
import org.jetbrains.kotlin.abi.tools.naming.jvmTypeDescToCanonical
import org.jetbrains.kotlin.abi.tools.naming.metadataNameToQualified
import kotlin.test.Test
import kotlin.test.assertEquals

class NamingTests {
    @Test
    fun testJvmInternalNameToCanonical() {
        assertEquals("foo.bar" to "Biz.Gz", "foo/bar/Biz\$Gz".jvmInternalToCanonical())
        assertEquals("foo.bar" to "Biz.\$\$Gz.\$X", "foo/bar/Biz\$\$\$Gz\$\$X".jvmInternalToCanonical())
        assertEquals("foo.bar" to "Biz", "foo/bar/Biz".jvmInternalToCanonical())
        assertEquals("" to "Foo.Bar", "Foo\$Bar".jvmInternalToCanonical())
        assertEquals("" to "Foo.Bar\$", "Foo\$Bar\$".jvmInternalToCanonical())
        assertEquals("" to "Foo", "Foo".jvmInternalToCanonical())
    }

    @Test
    fun testParameterDescToCanonical() {
        assertEquals("foo.bar" to "Biz.Gz", "Lfoo/bar/Biz\$Gz;".jvmTypeDescToCanonical())
        assertEquals("foo.bar" to "Biz.\$\$Gz.\$X", "Lfoo/bar/Biz\$\$\$Gz\$\$X;".jvmTypeDescToCanonical())
        assertEquals("foo.bar" to "Biz", "Lfoo/bar/Biz;".jvmTypeDescToCanonical())
        assertEquals("" to "Foo.Bar", "LFoo\$Bar;".jvmTypeDescToCanonical())
        assertEquals("" to "Foo.Bar\$", "LFoo\$Bar\$;".jvmTypeDescToCanonical())
        assertEquals("" to "Foo", "LFoo;".jvmTypeDescToCanonical())
    }

    @Test
    fun test() {
        assertEquals("foo.bar" to "Biz", "foo/bar/Biz".metadataNameToQualified())
        assertEquals("foo.bar" to "Biz.Gz", "foo/bar/Biz.Gz".metadataNameToQualified())
        assertEquals("foo.bar" to "\$Biz.\$Gz", "foo/bar/\$Biz.\$Gz".metadataNameToQualified())
        assertEquals("foo.bar" to "\$Biz.\$Gz\$", "foo/bar/\$Biz.\$Gz\$".metadataNameToQualified())
        assertEquals("" to "Foo", "Foo".metadataNameToQualified())
    }
}