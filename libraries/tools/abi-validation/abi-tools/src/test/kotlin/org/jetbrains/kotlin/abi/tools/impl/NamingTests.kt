package org.jetbrains.kotlin.abi.tools.impl

import org.jetbrains.kotlin.abi.tools.impl.naming.jvmInternalToCanonical
import org.jetbrains.kotlin.abi.tools.impl.naming.jvmTypeDescToCanonical
import org.jetbrains.kotlin.abi.tools.impl.naming.metadataNameToQualified
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