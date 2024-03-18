/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalLibraryAbiReader::class)

package kotlinx.validation.api.tests

import kotlinx.validation.api.klib.toAbiQualifiedName
import org.jetbrains.kotlin.library.abi.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClassNameConvertionTest {
    @Test
    fun testConvertBinaryName() {
        assertNull("".toAbiQualifiedName())
        assertNull("   ".toAbiQualifiedName())
        assertNull("a/b/c/d".toAbiQualifiedName())
        assertNull("a.b.c/d.e".toAbiQualifiedName())

        checkNames("Hello", AbiQualifiedName("", "Hello"))
        checkNames("a.b.c", AbiQualifiedName("a.b", "c"))
        checkNames("a\$b\$c", AbiQualifiedName("", "a.b.c"))
        checkNames("p.a\$b\$c", AbiQualifiedName("p", "a.b.c"))
        checkNames("org.example.Outer\$Inner\$\$serializer",
            AbiQualifiedName("org.example", "Outer.Inner.\$serializer"))
        checkNames("org.example.Outer\$Inner\$\$\$serializer",
            AbiQualifiedName("org.example", "Outer.Inner.\$\$serializer"))
        checkNames("a.b.e.s.c.MapStream\$Stream\$",
            AbiQualifiedName("a.b.e.s.c", "MapStream.Stream\$"))
    }

    private fun checkNames(binaryClassName: String, qualifiedName: AbiQualifiedName) {
        val converted = binaryClassName.toAbiQualifiedName()!!
        assertEquals(qualifiedName.packageName, converted.packageName)
        assertEquals(qualifiedName.relativeName, converted.relativeName)
    }
}

private fun AbiQualifiedName(packageName: String, className: String) =
    AbiQualifiedName(AbiCompoundName(packageName), AbiCompoundName(className))
